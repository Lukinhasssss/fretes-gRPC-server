package br.com.lukinhasssss

import com.google.protobuf.Any
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FretesGrpcServer : FretesServiceGrpc.FretesServiceImplBase() {

    private val logger = LoggerFactory.getLogger(FretesGrpcServer::class.java)

    override fun calculaFrete(request: CalculaFreteRequest?, responseObserver: StreamObserver<CalculaFreteResponse>?) {
        logger.info("Calculando frete para request: $request")

        val cep = request?.cep

        if (cep == null || cep.isBlank()) {
//            val error = StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Cep deve ser informado"))
            val error = Status.INVALID_ARGUMENT.withDescription("Cep deve ser informado").asRuntimeException()
            responseObserver?.onError(error)
        }

        if (!cep!!.matches("[0-9]{5}-[0-9]{3}".toRegex()))
            responseObserver?.onError(Status.INVALID_ARGUMENT
                .withDescription("Cep inválido!")
                .augmentDescription("Formato esperado deve ser 99999-999")
                .asRuntimeException())

        // SIMULANDO UMA VERIFICAÇÃO DE SEGURANÇA
        if (cep.endsWith("999")) {
            val statusProto = com.google.rpc.Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED_VALUE)
                .setMessage("Usuario nao pode acessar esse recurso")
                .addDetails(Any.pack(ErrorDetails.newBuilder()
                    .setCode(401)
                    .setMessage("token expirado")
                    .build()))
                .build()
            val error = StatusProto.toStatusRuntimeException(statusProto)
            responseObserver?.onError(error)
        }

        var valor = 0.0
        try {
            valor = Random.nextDouble(from = 0.0, until = 140.0)

            if (valor > 100.0)
                throw IllegalStateException("Erro inesperado ao executar logica de negocio!")
        } catch (e: Exception) {
            responseObserver?.onError(Status.INTERNAL
                .withDescription(e.message)
                .withCause(e) // anexado ao Status, mas não é enviado ao Client
                .asRuntimeException())
        }

        val response = CalculaFreteResponse.newBuilder()
            .setCep(request.cep)
            .setValor(Random.nextDouble(from = 0.0, until = 140.0))
            .build()

        logger.info("Frete calculado: $response")

        responseObserver!!.onNext(response)
        responseObserver.onCompleted()
    }

}