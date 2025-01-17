pipeline {
    agent any
    
    environment {
        DOMAIN = 'docker-swarm-visualizer'
        STACK_NAME = 'docker-swarm-visualizer'

        DOCKER_HUB_CREDENTIALS = 'DockerHub'
        TAG_NAME = 'latest'
        SSH_USER = 'root'
        SSH_HOST = '91.107.199.72'
        SSH_PORT = '22'
        WAIT_TIME = 30

        SOURCE_REPO_URL = "https://github.com/rdnsx/${DOMAIN}.git"
        DOCKER_IMAGE_NAME = "rdnsx/${STACK_NAME}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', credentialsId: 'Git', url: env.SOURCE_REPO_URL
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    def buildNumber = env.BUILD_NUMBER
                    docker.withRegistry('', DOCKER_HUB_CREDENTIALS) {
                        def dockerImage = docker.build("${DOCKER_IMAGE_NAME}:${buildNumber}", ".")
                        dockerImage.push()

                        dockerImage.tag("latest")
                        dockerImage.push("latest")
                    }
                }
            }
        }
        
        stage('Deploy to Swarm') {
            steps {
                script {
                    sshagent(['Swarm00']) {
                        sh """
                           ssh -o StrictHostKeyChecking=no -p ${SSH_PORT} ${SSH_USER}@${SSH_HOST} '
                           mount -a &&
                           cd /mnt/SSS/DockerCompose/ &&
                          rm -rf ${DOMAIN}/ &&
                          mkdir ${DOMAIN}/ &&
                         cd ${DOMAIN}/ &&
                          curl -H -v -o docker-compose-swarm.yml https://raw.githubusercontent.com/rdnsx/${DOMAIN}/master/docker-compose-swarm.yml &&
                          docker stack deploy -c docker-compose-swarm.yml dockerswarmvisualizer;'
                        """
                    }
                }
            }
        }
    }
}