#!/bin/sh

registryname=vine-registry-dev.np-0000188.npause1.bakerhughes.com
project=VINE    #enter you project name
env=DEV        #enter your env name

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

# Setup kubernetes
setup_kubernetes() {
    set +e

    echo "Create Kubernetes Repo"
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

    cat << EOF > /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
EOF

    sysctl --system

    echo "Disable SE Linux"
    sudo setenforce 0
    sudo sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config

    echo "Start Installing Kubernetes "
    yum install -y kubelet-1.22.2 kubeadm-1.22.2 kubectl-1.22.2 --disableexcludes=kubernetes
    systemctl enable --now kubelet
    #systemctl start kubelet

    yes | kubeadm reset
	
    echo "Initializing Kubernetes Master"
    kubeadm init --token-ttl 0 --pod-network-cidr=192.168.0.0/16 --ignore-preflight-errors=all

    systemctl daemon-reload
    systemctl restart kubelet

    echo "Copying admin.conf file to home directory"
    mkdir -p $HOME/.kube | true
    yes | cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
    chown $(id -u):$(id -g) $HOME/.kube/config

    yes | cp /etc/kubernetes/admin.conf $HOME/
    chown $(id -u):$(id -g) $HOME/admin.conf
    export KUBECONFIG=$HOME/admin.conf

    #curl https://raw.githubusercontent.com/projectcalico/canal/master/k8s-install/1.7/rbac.yaml > rbac.yaml
    #kubectl apply -f rbac.yaml
	
    echo "Deploy Overlay network Canal"
    #curl https://docs.projectcalico.org/v3.11/manifests/calico.yaml > calico.yaml
    curl https://docs.projectcalico.org/manifests/canal.yaml -O
    kubectl apply -f canal.yaml

    kill_process "kubectl"

    nohup kubectl proxy --address=0.0.0.0 --port=80 --accept-hosts='^*$' &


    set -e
}


#Setup kubernetes
setup_kubernetes

echo "Generate join token"
join_command=$(kubeadm token create --ttl 0 --print-join-command)

if [[ -z "${join_command}" || "${join_command}" == "null" ]]
then
    err "$(caller) Unable to get join command. Exiting"
    exit 1
fi
echo "Printing Join Command"
echo $join_command
echo $join_command > join-cmd.txt
curl -u 502712493:AP6XeMroj4KAkFt1psdkSebGHFN -X PUT "https://artifactory.ops.bhge.ai/artifactory/bh-mumbai/$project/$env/join-cmd.txt" -T join-cmd.txt

kubectl get nodes