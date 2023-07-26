	def userInput
	def buildVersion = ''
	def git_url = 'https://github.com/bh-ent-tech/iquity-technician-console-app.git' // update gitrepo details accordingly in git_url variable to checkout the code.  
	def to_list = 'BH_Digital_DevOps@bakerhughes.com' // Please replace the to_list with required email ids.
	pipeline{
		agent {
			node{
				label 'BH-e2c-np190'
				//customWorkspace '/home/jenkins/workspace/mtc_jobs/Alerts2/bh-devops-CI-workspace/'
			}
		}
	// customWorkspace to be defined in case of using camav scan.    
		options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '30'))
	  } 

		parameters {
			//string(name: 'Major', defaultValue: '1', description: 'provide the Major version')
			//string(name: 'Minor', defaultValue: '2', description: 'provide the Minor version')
			//string(name: 'Revision', defaultValue: '', description: 'Keep it empty if BuildType is Normal. Provide Revision if BulidType is Patch')
			//choice(name: 'BuildType', choices: ['Normal','Patch','Patch'])
			//string(name: 'image_build', defaultValue: 'ogd-cf-edge-device-simulator', description: 'provide the name of the image to be built')
			  //string(name: 'registry_name', defaultValue: 'e2c-registry-dev.np-0000173.npause1.bakerhughes.com', description: 'provide the docker registry name to push the image')
			choice(name: 'Branch_to_Build', choices: ['develop','master','venkat-bh-patch-1','DE119761','US438263'] , description: 'branch to be checked out for building the code')
		}
	/*
	 define the credentials to be used according to the project requirements in environment.
	 
	 */
		environment {
			//cdecred = credentials('e2c-dev-registry')
			gitcred = credentials('bh-git')
			blackduck = credentials('BLACKDUCKID')

		}
		stages{
			/*stage('Check prerequisites'){
				steps{
					script{
						
					 if ("${params.image_build}" == ''){
						currentBuild.result = 'ABORTED'
						error('Image name not provided !!!')
						}
					}
				}
			}*/
	// update gitrepo details accordingly in git_url variable defined on top, to checkout the code.        
			stage("Git Checkout")
			{
				steps{
					git branch: "${Branch_to_Build}" , credentialsId: 'bh-git', url: "${git_url}"
				}
			}

	//change the agent docker image in case of different maven version to be used.  

        stage('NPM Build'){
           steps {
              withCredentials([file(credentialsId: 'npmrc-gitpkgs-sps', variable: 'npmrc')]){     
                sh '''             
                    source ~/.bashrc            
                    echo ${WORKSPACE}
                    cat $npmrc>.npmrc
                    
		    #npm cache clean --force
		    source ~/.nvm/nvm.sh
                    nvm use 17.9.1
                    node -v && npm -v
                    npm install --progress=false --unsafe-perm
                    npm install -g gulp
                    gulp build-prod
                    npm prune --production
                    #npm run test
                    #npm run test-coverage
                    #npm run sonar
                    
                    '''
                }
            }
        } 
    //    stage('Checkmarx') {
                          
    //              steps {
    //                     echo "  Executing Checkmarx Jenkins Plugin to request a Scan..." 		
    //                     step([$class: 'CxScanBuilder',
    //                     comment: '',
    //                     sastEnabled: 'true',
    //                     customFields: '', 
    //                     excludeFolders: '', 
    //                     exclusionsSetting: '', 
    //                     failBuildOnNewResults: false,
    //                     failBuildOnNewSeverity: 'HIGH', 
    //                     filterPattern: '''!**/_cvs/**/*, !**/.svn/**/*,  !**/.hg/**/*,  !**/.git/**/*, !**/.bzr/**/*, !**/bin/**/*,
    //                     !**/obj/**/*, !**/backup/**/*, !**/.idea/**/*, !**/*.DS_Store, !**/*.ipr,   !**/*.iws,
    //                     !**/*.bak,   !**/*.tmp,    !**/*.aac,   !**/*.aif,   !**/*.iff,   !**/*.m3u, !**/*.mid, !**/*.mp3,
    //                     !**/*.mpa,   !**/*.ra,    !**/*.wav,   !**/*.wma,   !**/*.3g2,   !**/*.3gp, !**/*.asf, !**/*.asx,
    //                     !**/*.avi,   !**/*.flv,    !**/*.mov,   !**/*.mp4,   !**/*.mpg,   !**/*.rm, !**/*.swf, !**/*.vob,
    //                     !**/*.wmv,   !**/*.bmp,    !**/*.gif,   !**/*.jpg,   !**/*.png,   !**/*.psd, !**/*.tif, !**/*.swf,
    //                     !**/*.jar,   !**/*.zip,    !**/*.rar,   !**/*.exe,   !**/*.dll,   !**/*.pdb, !**/*.7z, !**/*.gz,
    //                     !**/*.tar.gz, !**/*.tar,    !**/*.gz,    !**/*.ahtm,   !**/*.ahtml,  !**/*.fhtml, !**/*.hdm,
    //                     !**/*.hdml,  !**/*.hsql,   !**/*.ht,    !**/*.hta,   !**/*.htc,   !**/*.htd, !**/*.war, !**/*.ear,
    //                     !**/*.htmls,  !**/*.ihtml,   !**/*.mht,   !**/*.mhtm,   !**/*.mhtml,  !**/*.ssi, !**/*.stm,
    //                     !**/*.stml,  !**/*.ttml,   !**/*.txn,   !**/*.xhtm,   !**/*.xhtml,  !**/*.class, !**/*.iml, !Checkmarx/Reports/*.*''',
    //                     fullScanCycle: 10,
    //                     includeOpenSourceFolders: '',
    //                     osaArchiveIncludePatterns: '*.zip, *.war, *.ear, *.tgz',
    //                     osaInstallBeforeScan: false,
    //                     isProxy: false,
    //                     useOwnServerCredentials: true,
    //                     credentialsId: 'svc-e2c-platform',
    //                     preset: '0',
    //                     projectName: '${image_build}',
    //                     serverUrl: 'https://checkmarx.prd-0000102.pause1.bakerhughes.com',
    //                     sourceEncoding: '1',
    //                     vulnerabilityThresholdResult: 'FAILURE',
    //                     waitForResultsEnabled: true])
    //             }
    //        }					
	/*
	this stage is for checking the sonar results. Update the pom.xml with the required plugins needed for sonar analysis.
	You can enable this stage if sonarqube setting is added in your source code.

		stage('SonarQube Analysis') {
		  steps {
			script {
			  try {
				withSonarQubeEnv('SONAR_URL') {
				sh """ mvn clean install org.sonarsource.scanner.maven:sonar-maven-plugin:3.8:sonar  """              
				  }   
				}
			  catch (Throwable err) {
			  currentBuild.result = 'FAILED'
			  error("SonarQube Analysis FAILED")
			  }
			}
		  }
		}
	*/

/*
	 //use this stage for triggering the clamav scan for java projects.
	 
			 stage('Scanning the Artifacts using clamav scan')
			{  
				steps{
					script{
					
					sh '''
					pwd
					echo ${WORKSPACE}
					ls *.jar | tee bldjars.txt
					cat bldjars.txt
					while IFS= read -r line; do 
					ls -l $line
					curl -s -XPOST http://172.16.240.214:8080/api/v1/scan -F FILES=@./${line} | jq . >> scan.out
					flg=$(grep -w "is_infected" scan.out | awk '{ print $2}'| sed 's/,//g')
					echo $flg 
					if [ -z "$flg" ];
					then
					{
						echo 'No value in flg'  
					} 
					elif [ "$flg" == "true" ];
					then
					{
						echo "Virus detected, hence aborting build. please check"
						exit 1
					}
					elif [ "$flg" == "false" ];
					then
					{
						echo "No Virus detected, hence proceeding build. "
						
					}
					
					else
						echo "None of the condition met"
					fi			
					done < bldjars.txt
					cat scan.out  
					'''
					}
				}
			}
*/


	/*For clam AV scan report we need to trigger a freestyle job which will be scanning the artifacts present in the custom workspace defined in agents. 
	Please refer avscan-report.doc within the documents folder for creating the freestyle job. 
	Path to the freestyle job needs to be updated accordingly in build job in below stage.*/

		/*	stage('Publish AV scan Report')
			{
				steps{
					echo "This stage is to trigger downstream job to publish Antivirus Scan Report"
					build job: 'avscan-report'
				  
				}
			}
*/		
	// use this stage to lint the dockerfile. If the path of Dockerfile is different update the commands accordingly, here we have dockerfile in base path.
	 
	stage ("lint dockerfile") {
		agent {
			docker {
				image 'hadolint/hadolint:latest-debian'
				reuseNode true
			}
		}
		steps {
			sh 'hadolint ./Dockerfile | tee -a dockerfile_lint.txt'
		}
		post {
			always {
				archiveArtifacts 'dockerfile_lint.txt'
			}
		}
	}
	/*stage('black_duck_test'){
            steps{
            synopsys_detect '''  --blackduck.url='https://bakerhughes.app.blackduck.com'  --blackduck.api.token='$blackduck_PSW' --detect.project.name='device-simulator' --detect.project.group.name='E2C' --detect.project.version.name='1.0'  '''
            }
        }*/
	/*
	Assuming the dockerfile is present in base path, otherwise update the build command accordingly.
	This stage do the docker build, tag and push to registry. It creates a git tag and push it in github for builds from master or main branches. 
	*/
			stage("Docker Image Build")
			{
				environment{
					giturl="${git_url}"
				}
				steps{
					script{
					wrap([$class: 'BuildUser']) {

				sh '''
				 docker build -t edge-core-techconsole:2.0.2.$BUILD_NUMBER -f Dockerfile .
                 touch image.txt || true
                 echo "edge-core-techconsole:2.0.2.$BUILD_NUMBER" > image.txt
                cat image.txt
                image_name=`cat image.txt`
                echo "$image_name"
                curl -X POST \
                https://e2c-qa.np-0000190.npause1.bakerhughes.com/uaa_service/uaa/auth/token \
                -H 'Authorization: Basic cGNzLXNkYy0tMGIxMTg3YmYtMDFjNi00MGM1LTlmZDUtOTJhMDFlZDQzZmU5' \
                -H 'Content-Type: application/json' \
                -H 'cache-control: no-cache' \
                --insecure \
                -d '{
                    "grant_type":"client_credentials",
                    "clientId":"pcs-sdc--0b1187bf-01c6-40c5-9fd5-92a01ed43fe9-c664f4ff-32f5-4c5e-8771-dd9d3d15f054",
                    "clientSecret":"RuWt1fzwQyct5xby9Im2qY97Q9995SE2KG1P4L3zcZw="
                    }' > abc.txt
                token=`cat abc.txt | jq -r .access_token`
                curl -X GET "https://e2c-qa.np-0000190.npause1.bakerhughes.com/token-generator-service/v1/aws-ecr/token?registrationId=deviceId&repositoryUrl=https://681300882714.dkr.ecr.us-east-1.amazonaws.com" -H "Content-type: application/json" -H "Accept: application/json" \
                --insecure -H "Authorization: Bearer $token" > encode.txt

                encode=`cat encode.txt | jq -r .authToken | base64 --decode`
                passwd=`echo $encode | cut -c 5-`
                docker login -u AWS -p $passwd https://681300882714.dkr.ecr.us-east-1.amazonaws.com
                docker login -u AWS -p $passwd https://681300882714.dkr.ecr.us-east-1.amazonaws.com
                docker tag edge-core-techconsole:2.0.2.$BUILD_NUMBER 681300882714.dkr.ecr.us-east-1.amazonaws.com/e2c-qa:edge-core-techconsole_v2.0.2.$BUILD_NUMBER
                docker push 681300882714.dkr.ecr.us-east-1.amazonaws.com/e2c-qa:edge-core-techconsole_v2.0.2.$BUILD_NUMBER
                docker rmi 681300882714.dkr.ecr.us-east-1.amazonaws.com/e2c-qa:edge-core-techconsole_v2.0.2.$BUILD_NUMBER

                #echo ('+++++++++++++++++++++++')
                 #touch image.txt || true
                 #echo "edge-core-techconsole:2.0.2.$BUILD_NUMBER" > image.txt
                #cat image.txt
                #image_name=`cat image.txt`
                #echo "$image_name"
				#}
				#fi
				#'''
				//buildVersion = readFile('buildversion.txt').trim()
				//currentBuild.displayName = "$image_build:$buildVersion"
				  
				}
				}
				}
			}
            stage('trivy scan') {
		    agent { docker 
              { image 'aquasec/trivy:latest'
                args '-v /var/run/docker.sock:/var/run/docker.sock --entrypoint='
                reuseNode true
               }
            }
            steps {
                script {
            sh '''
            image_name="`cat image.txt 2>&1`"
            echo $image_name
            apk add curl
            curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/html.tpl > html.tpl
            trivy image --ignore-unfixed --format template --template "@html.tpl" -o cve_report.html ${image_name}
            mkdir publish
            cp -r cve_report.html $WORKSPACE/publish
            
            '''
            publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'publish',
                    reportFiles: 'cve_report.html',
                    reportName: 'Trivy Scan',
                    reportTitles: 'Trivy Scan'
                  ])
                    }
                }
            }
            stage('Cleanup') {
			steps{
				script{
                    sh 'docker rmi "`cat image.txt 2>&1`"'                    
                }
            }
		}
	/* This stage checks the vulnerability within the docker images using anchor scan. 
	By default the job fails if there is any high or Critical vulnerability, in order to bypass it please set bailOnFail: false
	
		stage('Vulnerability Scan with Anchore') {
				environment {
				imageLine = "${registry_name}/${image_build}:${buildVersion}"
				}

			  steps {


			writeFile file: 'anchore_images', text: imageLine
			anchore name: 'anchore_images',  bailOnFail: false

			  }
			}*/
		
	/*
	You can enable this stage if you want to update the image version in deployment repo. 
	This stage will update the yaml files in github and deploy it using argocd. 
	Please create the job with name update_deployment_manifest using the CD/Jenkinsfilecd-with-argocd-manifest-update script.
	update_deployment_manifest job path needs to be updated accordingly.*/

			/*stage('Update Manifest'){
				steps {
					script {
							echo "Proceeding with comitting the changes on GitHub..."
							build job: 'MTC_Jobs/E2C/E2C_Dev/update_deployment_manifest_dev-1', 
					parameters: [
			  string(name: 'IMAGE_NAME', value: "${image_build}"), string(name: 'IMAGE_TAG', value: "${buildVersion}"), string(name: 'REGISTRY', value: "${registry_name}"), string(name: 'DIR_NAME', value: "DEV")],
			  wait: false
						}
					}
				}*/
		    
		}
		// Workspace cleanup and email trigger.
		post { 
			always { 
		emailext (
			attachmentsPattern: 'dockerfile_lint.txt',
					subject: " job status is  ${currentBuild.currentResult} Job ${env.JOB_BASE_NAME} - Build # ${env.BUILD_NUMBER} ",
					body: """Status of ${env.JOB_BASE_NAME} BUILD # ${env.BUILD_NUMBER} is ${currentBuild.currentResult}, You can check console output at ${env.BUILD_URL}console
					""",
					to: "${to_list}"
				)
				cleanWs()
			
			  }
		}
	}
