#!/usr/bin/env bash

set -e

echo "🚀 Cosmos DB Query Sidecar - Build & Run Script"
echo "================================================"

# Function to display usage
usage() {
    echo "Usage: $0 [build|run|docker-build|docker-run|test]"
    echo ""
    echo "Commands:"
    echo "  build         - Build the JAR file using Gradle"
    echo "  run           - Run the application locally"
    echo "  docker-build  - Build Docker image"
    echo "  docker-run    - Run Docker container"
    echo "  test          - Run tests"
    exit 1
}

# Check if command is provided
if [ $# -eq 0 ]; then
    usage
fi

COMMAND=$1

case $COMMAND in
    build)
        echo "📦 Building JAR file..."
        ./gradlew clean build -x test
        echo "✅ Build complete! JAR file at: build/libs/cosmosdb-query-sidecar-1.0.0.jar"
        ;;
    
    run)
        echo "🏃 Running application locally..."
        if [ ! -f .env ]; then
            echo "⚠️  Warning: .env file not found. Copy .env.example to .env and configure it."
            exit 1
        fi
        source .env
        ./gradlew bootRun
        ;;
    
    docker-build)
        echo "🐳 Building Docker image..."
        echo "Building JAR first..."
        ./gradlew clean build -x test
        echo "Building Docker image with pre-built JAR..."
        docker build -f Dockerfile.prebuilt -t cosmosdb-query-sidecar:1.0.0 .
        echo "✅ Docker image built: cosmosdb-query-sidecar:1.0.0"
        ;;
    
    docker-run)
        echo "🐳 Running Docker container..."
        if [ ! -f .env ]; then
            echo "⚠️  Warning: .env file not found. Copy .env.example to .env and configure it."
            exit 1
        fi
        docker-compose up
        ;;
    
    test)
        echo "🧪 Running tests..."
        ./gradlew test
        ;;
    
    *)
        echo "❌ Unknown command: $COMMAND"
        usage
        ;;
esac
