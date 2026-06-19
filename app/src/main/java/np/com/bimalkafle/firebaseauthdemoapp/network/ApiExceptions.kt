package np.com.bimalkafle.firebaseauthdemoapp.network

/** Backend rejected the request because the Firebase token is missing, invalid, or expired. */
class UnauthorizedException(message: String = "Session expired") : Exception(message)

/** Backend returned a non-2xx response that wasn't an auth failure. */
class ServerException(val httpCode: Int, message: String) : Exception(message)

/** Response body could not be parsed as JSON. */
class MalformedResponseException(message: String, cause: Throwable? = null) : Exception(message, cause)
