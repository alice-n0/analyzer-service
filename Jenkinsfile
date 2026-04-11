pipeline {
    agent any

    tools {
        jdk 'jdk-17'
    }

    options {
        timestamps()
    }

    parameters {
        string(
                name: 'DOCKER_REPO',
                defaultValue: 'hyeonjin5012',
                trim: true,
                description: 'Docker Hub 사용자/조직 (이미지: DOCKER_REPO/analyzer-service)'
        )
        string(
                name: 'NAMESPACE',
                defaultValue: 'observability-platform',
                trim: true,
                description: 'Kubernetes 네임스페이스 (kubectl apply / set image)'
        )
    }

    environment {
        IMAGE_TAG = "${BUILD_NUMBER}"
        DOCKER_IMAGE = "${params.DOCKER_REPO}/analyzer-service"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Gradle Build') {
            steps {
                sh '''
                    chmod +x ./gradlew 2>/dev/null || true
                    ./gradlew clean build --no-daemon
                '''
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    if (!params.DOCKER_REPO?.trim()) {
                        error('DOCKER_REPO 파라미터가 비어 있습니다.')
                    }
                    def image = docker.build("${env.DOCKER_IMAGE}:${env.IMAGE_TAG}")
                    docker.withRegistry('', 'docker_password') {
                        image.push("${env.IMAGE_TAG}")
                        image.push('latest')
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    set -e
                    kubectl apply -f k8s/deployment.yaml -f k8s/service.yaml -n ${params.NAMESPACE}
                    kubectl set image deployment/analyzer-service analyzer-service=${env.DOCKER_IMAGE}:${env.IMAGE_TAG} -n ${params.NAMESPACE} --record
                    kubectl rollout status deployment/analyzer-service -n ${params.NAMESPACE} --timeout=180s
                """
            }
        }
    }

    post {
        success {
            echo "✅ analyzer-service 배포 성공 (${env.DOCKER_IMAGE}:${env.IMAGE_TAG} → ${params.NAMESPACE})"
        }
        failure {
            echo "❌ analyzer-service 배포 실패"
        }
    }
}
