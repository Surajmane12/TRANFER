import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class FileTransferServer(private val port: Int) {

    fun start() {
        val serverSocket = ServerSocket(port)
        println("Server started on port $port")

        while (true) {
            val clientSocket = serverSocket.accept()
            println("Client connected: ${clientSocket.inetAddress.hostAddress}:${clientSocket.port}")
            thread { handleClient(clientSocket) }
        }
    }

    private fun handleClient(socket: Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = DataOutputStream(socket.getOutputStream())

        try {
            while (true) {
                val commandLine = input.readLine() ?: break
                println("Received command: $commandLine")

                when {
                    commandLine.startsWith("UPLOAD") -> {
                        val fileName = commandLine.substringAfter(" ")
                        receiveFile(fileName, socket.getInputStream())
                        println("File uploaded: $fileName")
                        output.writeBytes("OK\n")
                    }
                    commandLine.startsWith("DOWNLOAD") -> {
                        val fileName = commandLine.substringAfter(" ")
                        sendFile(fileName, output)
                    }
                    else -> {
                        output.writeBytes("ERROR: Unknown command\n")
                    }
                }
            }
        } catch (e: IOException) {
            println("Client disconnected: ${socket.inetAddress.hostAddress}:${socket.port}")
        } finally {
            socket.close()
        }
    }

    private fun receiveFile(fileName: String, input: InputStream) {
        val file = File("server_storage/$fileName")
        file.parentFile.mkdirs()  // Ensure the directory exists

        FileOutputStream(file).use { fos ->
            val buffer = ByteArray(4096)
            var bytesRead: Int

            // Read the file data from the input stream
            while (input.read(buffer).also { bytesRead = it } != -1) {
                fos.write(buffer, 0, bytesRead)

                // Break if the end of the file is reached
                if (bytesRead < buffer.size) break
            }
        }
    }

    private fun sendFile(fileName: String, output: DataOutputStream) {
        val file = File("server_storage/$fileName")
        if (file.exists()) {
            output.writeBytes("OK\n")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            println("File sent: $fileName")
        } else {
            output.writeBytes("ERROR: File not found\n")
            println("Requested file not found: $fileName")
        }
    }
}

fun main() {
    val server = FileTransferServer(port = 12345)
    server.start()
}
