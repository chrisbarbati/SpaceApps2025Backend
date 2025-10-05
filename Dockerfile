# Use a base image with Java 24
FROM openjdk:24-jdk-bookworm

# Set environment variables
ENV PYTHONUNBUFFERED=1

# Install Python 3 and pip
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the Spring Boot jar into the container
# Replace 'your-app.jar' with the actual jar name
COPY SpaceApps2025Backend.jar app.jar

# Install required pip dependencies
# Replace requirements.txt with your file if needed
COPY requirements.txt .
RUN pip3 install --no-cache-dir --break-system-packages -r requirements.txt

# Expose the port your Spring Boot app runs on
EXPOSE 8080:8080

# Run the application with prod profile
ENTRYPOINT ["java", "-jar", "app.jar"]