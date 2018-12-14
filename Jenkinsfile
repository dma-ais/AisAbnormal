pipeline {
    agent any

    tools {
        maven 'M3.3.9'
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('build') {
            steps {
                withMaven(options: [junitPublisher(ignoreAttachments: false), artifactsPublisher()]) {
                    sh 'mvn -DskipTests -DincludeSrcJavadocs -DskipITs clean source:jar install checkstyle:check'
                }
            }
            post {
                success {
                    archiveArtifacts(artifacts: '**/target/*-bundle.zip', allowEmptyArchive: true)
                    sh 'curl --data "build=true" -X POST https://registry.hub.docker.com/u/dmadk/ais-ab-analyzer/trigger/8eb8199e-4490-11e4-a927-4646782c2ba9/'
                    sh 'curl --data "build=true" -X POST https://registry.hub.docker.com/u/dmadk/ais-ab-web/trigger/e19810a6-3ff9-11e4-8b16-f22651b4a814/'

                }
            }
        }
    }

    post {
        failure {
            // notify users when the Pipeline fails
            mail to: 'steen@lundogbendsen.dk,tbsalling@tbsalling.dk',
                    subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                    body: "Something is wrong with ${env.BUILD_URL}"
        }

    }
}
