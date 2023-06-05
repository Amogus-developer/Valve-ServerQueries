import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SourceEngineClient: Closeable {

    private val socket = DatagramSocket()
    private val defaultRequest = createRequest()



    fun printInfo(address: String, port: Int) {
        printInfo(InetAddress.getByName(address), port, 0)
    }

    private fun printInfo(address: InetAddress, port: Int, challenge: Long, retries: Int = 0) {
        val request = if (challenge == 0L) defaultRequest else createRequest(challenge)
        var packet = DatagramPacket(request, request.size, address, port)
        socket.send(packet)
        val buffer = ByteArray(65535)
        packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        val reader = DataReader(ByteArrayInputStream(buffer))

        if (reader.readUInt32() != 4294967295L)
            throw IllegalStateException("Разрывная xD!")

        val responseType = reader.readUInt8()
        if (responseType == 0x41) {
            if (retries == 10)
                throw IllegalStateException("Failed after 10 retries!") else
                printInfo(address, port, reader.readUInt32(), retries + 1)
            return
        }

        if (responseType != 0x49)
            throw IllegalStateException("Legacy shit (гавно мамонта)!")

        println("Protocol: ${reader.readUInt8()}")
        println("Server name: ${reader.readUTF8()}")
        println("Map name: ${reader.readUTF8()}")
        println("Folder: ${reader.readUTF8()}")
        println("Game: ${reader.readUTF8()}")
        println("App id: ${reader.readUInt16()}")
        println("Player count: ${reader.readUInt8()}")
        println("Max players: ${reader.readUInt8()}")
    }



    override fun close() {
        socket.close()
    }



    companion object {

        private val TEXT = createTextBytes("Source Engine Query")



        private fun createRequest(challenge: Long = 0L): ByteArray {
            val output = ByteArrayOutputStream()
            output.write(-1)
            output.write(-1)
            output.write(-1)
            output.write(-1)
            output.write(0x54)
            output.write(TEXT)
            if (challenge != 0L) DataWriter(output).writeUInt32(challenge)
            return output.toByteArray()
        }

        private fun createTextBytes(text: String): ByteArray {
            val output = ByteArrayOutputStream()
            val writer = DataWriter(output)
            writer.writeUTF8(text)
            return output.toByteArray()
        }

    }
}