# Pull base image
FROM yti-docker-java11-base:alpine

# Deploy project
ADD target/yti-codelist-content-intake-service.jar ${deploy_dir}/yti-codelist-content-intake-service.jar

# Expose port
EXPOSE 9602

# Set default command on run
ENTRYPOINT ["/bootstrap.sh", "yti-codelist-content-intake-service.jar"]
