package buy01.gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class GatewayServiceApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
                .directory("../") // adjust relative path if needed
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );
		SpringApplication.run(GatewayServiceApplication.class, args);
	}

}
