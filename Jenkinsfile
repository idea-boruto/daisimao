pipeline {
    agent any

    stages {
        stage('部署到服务器') {
            steps {
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: 'target-server',
                            transfers: [
                                sshTransfer(
                                    execCommand: '''
                                        cd /www/dk_project/daisimao
                                        chmod +x deploy.sh
                                        bash deploy.sh
                                    '''
                                )
                            ]
                        )
                    ]
                )
            }
        }
    }

    post {
        success {
            echo '🎉 部署成功！访问 http://150.158.108.241'
        }
        failure {
            echo '❌ 部署失败，请检查日志'
        }
    }
}
