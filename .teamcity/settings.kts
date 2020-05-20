package _Self.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

object BuildTest : BuildType({
    name = "Build Test"

    }
    steps {
        script {
            name = "random test"
            enabled = false
            scriptContent = "service docker status"
        }
        step {
            name = "install dependency"
            type = "jonnyzzz.nvm"
            enabled = false
            param("version", "13.6.0")
        }
        step {
            name = "Install modules and run test"
            type = "jonnyzzz.npm"
            enabled = false
            param("npm_commands", "install")
        }
        dockerCommand {
            name = "Docker Build image"
            enabled = false
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "gcr.io/%PROJECT_ID%/node-app:dev-%build.counter%"
                commandArgs = "--pull"
            }
            param("dockerImage.platform", "linux")
        }
        script {
            name = "service account authentication"
            enabled = false
            scriptContent = """
                echo '%GCLOUD_KEY%' > ./new-file.json
                gcloud auth activate-service-account --key-file=./new-file.json --project=%PROJECT_ID%
                gcloud container clusters get-credentials %CLUSTER_NAME% --zone us-central1-c --project %PROJECT_ID%
                kubectl config current-context
                kubectl config set-context --current --namespace=mitesh
                gcloud auth configure-docker
            """.trimIndent()
        }
        dockerCommand {
            name = "Docker Push"
            enabled = false
            commandType = push {
                namesAndTags = "gcr.io/%PROJECT_ID%/node-app:dev-%build.counter%"
                removeImageAfterPush = false
            }
        }
        script {
            name = "deploy image to kubernetes"
            enabled = false
            scriptContent = "IMAGETAG='%IMAGE_TAG%' node deploy-apps.js"
        }
    }
    triggers {
        vcs {
            enabled = false
            branchFilter = ""
        }
    }
})
