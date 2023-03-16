#!/usr/bin/env groovy

def stagePrepare(apps, parallelExecuteCount) {
    buildStageList = []
    buildParallelMap = [:]
    apps.eachWithIndex { app, value, i ->
        Integer lock_id = i % parallelExecuteCount
        println lock_id
        buildParallelMap.put(app, stageCreate(app, value, lock_id))
    }
    buildStageList.add(buildParallelMap)
    return buildStageList
}

def stageCreate (app, value, lock_id) {
    return {
        stage(app) {
            lock("Build-lock-${lock_id}") {
                dir(value.path) {
                    sh "docker build -t ${app} -f Dockerfile ."
                }
            }
        }
    }
}

def getPrId(branch) {
    if (branch =~ /PR-*/) {
        prId = env.CHANGE_ID
    } else {
        prId = "None"
    }
    return prId
}

def httpRequestGet (requestURL, pass) {
    try {
        def url = new URL(requestURL)
        HttpURLConnection conn = (HttpURLConnection) url.openConnection()
        // conn = new URL(requestURL).openConnection()
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("Authorization", "Bearer ${pass}")
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

        def getRC = conn.getResponseCode()
        println ("Responce Code: ${getRC}")
        if (getRC.equals(200)) {
            data = conn.content.text
        }
    } catch (err) {
        println "Failure get data from requestURL (${err.getMessage()})"
    }
    return data
}

def isChanged(prDiff, path) {
    for (file in prDiff) {
        if (file =~ path) {
            return true
        }
    }
}