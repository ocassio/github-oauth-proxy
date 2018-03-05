package ru.ionov

import spark.Request
import spark.Spark.get
import spark.Spark.post
import java.net.URLEncoder
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Form
import javax.ws.rs.core.MediaType

const val GITHUB_URL = "https://github.com"
const val AUTH_URL = "/login/oauth/authorize"
const val AUTH_CALLBACK_URL = "/login/github"
const val TOKEN_URL = "/login/oauth/access_token"

val client = ClientBuilder.newClient()!!
val authRequests = HashSet<Map<String, Array<String>>>()

fun main(args: Array<String>) {

    get(AUTH_URL) { request, response ->
        authRequests.add(request.queryMap().toMap())

        val updatedParams = request.queryMap().toMap()
        updatedParams["redirect_uri"] = arrayOf(getAbsolutePath(request, AUTH_CALLBACK_URL))
        val query = mapToQueryParams(updatedParams)

        response.redirect(GITHUB_URL + AUTH_URL + query)
    }

    get(AUTH_CALLBACK_URL) { request, response ->
        val params = request.queryMap().toMap()
        val iterator = authRequests.iterator()
        while (iterator.hasNext()) {
            val record = iterator.next()
            if (record["state"]!![0] == params["state"]!![0]) {
                iterator.remove()
                val query = mapToQueryParams(params)
                response.redirect(record["redirect_uri"]!![0] + query)
                break
            }
        }
    }

    post(TOKEN_URL) { request, response ->
        val params = request.queryMap().toMap()
        params["redirect_uri"] = arrayOf(getAbsolutePath(request, AUTH_CALLBACK_URL))

        val form = Form()
        params.forEach { key, value -> form.param(key, value[0]) }

        val resp = client.target(GITHUB_URL)
                .path(TOKEN_URL)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.form(form), String::class.java)

        response.type(MediaType.APPLICATION_JSON)
        resp
    }
}

fun getAbsolutePath(request: Request, url: String): String {
    return request.scheme() + "://" + request.host() + url
}

fun mapToQueryParams(map: Map<String, Array<String>>?): String {
    if (map == null || map.isEmpty()) {
        return ""
    }

    var result = "?"
    val iterator = map.iterator()
    while (iterator.hasNext()) {
        val entry = iterator.next()
        val value = URLEncoder.encode(entry.value[0], "UTF-8")
        result += "${entry.key}=$value"
        if (iterator.hasNext()) {
            result += "&"
        }
    }
    return result
}
