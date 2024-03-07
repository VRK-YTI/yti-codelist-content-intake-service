# Pull base image
FROM yti-docker-java-base:corretto-11.0.22

# Deploy project
ADD target/yti-codelist-content-intake-service.jar ${deploy_dir}/yti-codelist-content-intake-service.jar

# Expose port
EXPOSE 9602

# Set default command on run
ENTRYPOINT ["/bootstrap.sh", "yti-codelist-content-intake-service.jar"]
