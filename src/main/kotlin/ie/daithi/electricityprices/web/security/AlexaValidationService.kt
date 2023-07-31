package ie.daithi.electricityprices.web.security

import ie.daithi.electricityprices.model.alexa.AlexaRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import java.net.URL
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.*
import org.apache.tomcat.util.codec.binary.Base64
import java.security.Signature
import java.time.Duration

@Service
class AlexaValidationService {

    fun validate(request: HttpServletRequest, rawBody: String, body: AlexaRequest) {
        val signatureCertChainUrl = request.getHeader("SignatureCertChainUrl")
        val signature = request.getHeader("Signature")
        val timestamp = body.request?.timestamp

        validateUrl(signatureCertChainUrl)
        validateTimestamp(timestamp)

        val certificate = downloadCertificate(signatureCertChainUrl)
        validateCertificate(certificate)

        val signatureBytes = Base64.decodeBase64(signature)
        validateSignature(certificate, signatureBytes, rawBody.toByteArray(Charsets.UTF_8))
    }

    private fun validateUrl(signatureCertChainUrl: String) {
        val url = URL(signatureCertChainUrl)
        require(url.protocol == "https" && url.host == "s3.amazonaws.com" && url.path.startsWith("/echo.api/"))
    }

    private fun validateTimestamp(timestamp: String?) {
        require(timestamp != null)
        val requestTime = Instant.parse(timestamp)
        val currentTime = Instant.now()
        val duration = Duration.between(requestTime, currentTime)
        require(duration.seconds <= 150)
    }

    private fun downloadCertificate(signatureCertChainUrl: String): X509Certificate {
        val url = URL(signatureCertChainUrl)
        url.openConnection().getInputStream().use { certificateStream ->
            val certificateFactory = CertificateFactory.getInstance("X.509")
            return certificateFactory.generateCertificate(certificateStream) as X509Certificate
        }
    }

    private fun validateCertificate(certificate: X509Certificate) {
        certificate.checkValidity(Date.from(Instant.now()))
        require(certificate.subjectDN.name.contains("CN=echo-api.amazon.com"))
    }

    private fun validateSignature(certificate: X509Certificate, signature: ByteArray, data: ByteArray) {
        val signer = Signature.getInstance("SHA1withRSA")
        signer.initVerify(certificate)
        signer.update(data)
        require(signer.verify(signature))
    }
}
