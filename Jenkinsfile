pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Krishna8123/Chatpulse.git'
            }
        }

        stage('Build Maven Project') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                bat 'docker build -t chatpulse-app .'
            }
        }

        stage('Docker Compose Deploy') {
            steps {
                bat 'docker compose down -v'
                bat 'docker compose up -d --build'
            }
        }
    }
}
