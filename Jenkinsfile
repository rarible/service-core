@Library('shared-library') _

def credentialsId = 'nexus-ci'

pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    stages {
        stage('test') {
            steps {
                sh 'mvn clean test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
                }
            }
        }
        stage('deploy') {
            when {
                branch 'spring-2.5'
            }
            steps {
                deployToMaven(credentialsId)
            }
        }
    }
}
