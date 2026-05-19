pipeline {
    agent any

    stages {
        stage('拉取最新代码') {
            steps {
                echo '准备部署...'
            }
        }
        stage('在服务器上执行部署') {
            steps {
                sshCommand(
                    site: 'target-server',
                    command: 'cd /www/dk_project/daisimao && bash deploy.sh'
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
