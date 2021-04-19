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
				branch 'master'
			}
      steps {
				withCredentials([usernamePassword(credentialsId: 'jenkins-rarible-ci', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_TOKEN')]) {
					sh 'mvn deploy -DskipTests'
				}
      }
    }
  }
}
