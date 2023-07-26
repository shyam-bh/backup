#!/bin/sh

registryname=rdc-registry-qa.np-0000179.npause1.bakerhughes.com
project=rdc   #enter you project name
env=qa        #enter your env name

set +e

whoami

echo "Docker Registry name is $registryname"

#######################################
# Kill process by name
# Arguments:
#  $1: process_name
# Returns:
#   None
#######################################
kill_process() {
    pname="$1"
    pids=$(ps aux | grep "${pname}" | grep -v "grep" | awk '{print $2}')
    if [ ! -z "$pids" ];
    then
        kill -9 $pids
    fi

set -e

}

# initial config
initial_config() {
    set +e
    yum install -y yum-utils
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    yum -y update

    yum remove -y docker \
                  docker-common \
                  docker-selinux \
                  docker-engine

    yum remove docker-ce docker-ce-cli containerd.io -y

    yum install screen -y
    yum --nogpgcheck install docker-ce docker-ce-cli containerd.io -y
    yum -y install nfs-utils
    systemctl enable docker
    systemctl start docker
    docker_cmd="/usr/bin/dockerd --exec-opt native.cgroupdriver=systemd --insecure-registry=$registryname"
    sed -i "s,\(ExecStart=\).*\$,\1${docker_cmd}," /usr/lib/systemd/system/docker.service
    systemctl daemon-reload
    systemctl restart docker.service

    sysctl -w net.ipv4.ip_forward=1
	
    if [[ ! -f "/usr/bin/jq" ]];
    then
        wget http://stedolan.github.io/jq/download/linux64/jq
        chmod +x ./jq
        cp jq /usr/bin
    fi
    set -e

}

initial_config

# setup slave
setup_slave() {
    set +e
    sudo swapoff -a
    cat << EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
EOF
    echo "Disable SE Linux"
    sudo setenforce 0
    sudo sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config

    echo "Start Installing Kubernetes "
    yum install -y kubelet-1.23.5 kubeadm-1.23.5 kubectl-1.23.5 --disableexcludes=kubernetes
    systemctl enable --now kubelet
    #systemctl start kubelet

    echo "Kubeadm reset - required to delete old kubernetes files (if any)"
    yes | kubeadm reset

    set -e
}

setup_slave

systemctl daemon-reload
systemctl restart docker.service
systemctl restart kubelet

echo "========================"
echo "Execute Join command..."
echo "========================"
curl -u 502712493:AP6XeMroj4KAkFt1psdkSebGHFN -O "https://artifactory.ops.bhge.ai/artifactory/bh-mumbai/$project/$env/join-cmd.txt"
wait
sh join-cmd.txt
