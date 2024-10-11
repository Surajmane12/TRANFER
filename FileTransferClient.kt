import java.io.*
import java.net.*
import kotlin.concurrent.thread

class FileTransferClient(private val host: String, private val port: Int) {

    fun start() {
        val socket = Socket(host, port)
        println("Connected to server: $host:$port")
        val input = BufferedReader(InputStreamReader(System.`in`))
        val output = DataOutputStream(socket.getOutputStream())

        thread { receiveMessages(socket) }

        while (true) {
            print("Enter command (UPLOAD <filename> | DOWNLOAD <filename> | EXIT): ")
            val command = input.readLine() ?: break
            if (command == "EXIT") break

            when {
                command.startsWith("UPLOAD") -> {
                    val fileName = command.substringAfter(" ")
                    if (File(fileName).exists()) {
                        sendFile(fileName, output)
                    } else {
                        println("Error: File not found: $fileName")
                    }
                }
                command.startsWith("DOWNLOAD") -> {
                    val fileName = command.substringAfter(" ")
                    output.writeBytes(command + "\n")  // Notify the server to download
                    receiveFile(fileName, socket.getInputStream())
                }
                else -> {
                    println("Invalid command. Please use UPLOAD, DOWNLOAD, or EXIT.")
                }
            }
        }
        socket.close()
        println("Disconnected from server.")
    }

    private fun sendFile(fileName: String, output: DataOutputStream) {
        try {
            val file = File(fileName)
            output.writeBytes("UPLOAD $fileName\n")

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            println("Uploaded file: $fileName")
        } catch (e: IOException) {
            println("Error while uploading file: ${e.message}")
        }
    }

    private fun receiveFile(fileName: String, input: InputStream) {
        val response = BufferedReader(InputStreamReader(input)).readLine()
        if (response.startsWith("OK")) {
            try {
                val file = File("downloaded_$fileName")
                FileOutputStream(file).use { fos ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
                println("Downloaded file: ${file.absolutePath}")
            } catch (e: IOException) {
                println("Error while downloading file: ${e.message}")
            }
        } else {
            println("Server response: $response")
        }
    }

    private fun receiveMessages(socket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            var message: String?
            while (true) {
                message = input.readLine() ?: break
                println(message)
            }
        } catch (e: IOException) {
            println("Error receiving message: ${e.message}")
        }
    }
}

fun main() {
    val client = FileTransferClient(host = "127.0.0.1", port = 12345)
    client.start()
}
