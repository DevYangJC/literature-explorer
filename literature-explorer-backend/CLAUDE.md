# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a literature management system that provides AI-powered document analysis and reading guide generation. The system consists of:

- **Backend**: Spring Boot 3.5.5 application with H2 embedded database
- **Frontend**: Vue 3 application with Element Plus UI components
- **AI Integration**: Kimi AI API for document analysis and reading guide generation
- **File Processing**: Support for PDF, Word (.doc/.docx), and Markdown files

## Development Commands

### Quick Start (Recommended)

The project provides startup scripts that launch both backend and frontend simultaneously:

```bash
# Windows - Launches both backend (JAR) and frontend (Vue dev server)
start.bat

# Linux/macOS - Equivalent script
sh start.sh
```

**Important:** The startup script expects the compiled JAR file to be present at `./jar/literature-assistant-0.0.1-SNAPSHOT.jar`. If missing, build it first using `mvn clean package`.

### Backend (Java/Spring Boot)

```bash
# Start using pre-built JAR (fastest)
java -jar jar/literature-assistant-0.0.1-SNAPSHOT.jar

# Manual compilation and execution
mvn clean compile
mvn spring-boot:run

# Build JAR for production/deployment
mvn clean package

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=LiteratureServiceTest

# Run with specific Maven profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend (Vue.js)

```bash
cd literature-assistant-frontend

# Install dependencies (required after first clone)
npm install

# Start development server with hot reload
npm run dev

# Build for production
npm run build

# Preview production build locally
npm run preview

# Check for and fix issues
npm run lint  # If linting is configured
```

## Application Architecture

### Backend Architecture

The backend follows a layered architecture pattern:

- **Controller Layer**: REST API endpoints in `com.yuyuan.literature.controller`
  - `LiteratureController`: Main API for literature management operations
  - Provides SSE streaming for real-time AI processing feedback

- **Service Layer**: Business logic in `com.yuyuan.literature.service`
  - `LiteratureService`: Core literature management operations
  - `LiteratureAiService`: AI integration with Kimi API for analysis
  - `FileProcessingService`: File parsing and content extraction

- **Data Layer**: Data access and entities
  - `Literature` entity: Main domain model with MyBatis-Plus annotations
  - `LiteratureMapper`: Database operations with XML mapping
  - Uses H2 embedded database for persistence

- **Configuration**: Spring Boot configuration in `application.yml`
  - Custom properties under `literature.*` for file and AI settings
  - MyBatis-Plus for ORM, Knife4j for API documentation

### Frontend Architecture

The frontend uses Vue 3 with a component-based architecture:

- **Routing**: Vue Router with two main routes
  - `/` - Literature list view with filtering and search
  - `/literature/:id` - Literature detail view with reading guide display

- **State Management**: Pinia store (`literatureStore.js`)
  - Manages literature list, pagination, search filters
  - Handles API calls and error management

- **Key Components**:
  - `LiteratureListView`: Main list interface with table, filters, batch operations
  - `LiteratureDetailView`: Document details with reading guide
  - `ImportLiteratureModal`: Single file upload with real-time processing
  - `BatchImportModal`: Multiple file upload with progress tracking

### Key Technical Features

1. **AI Integration**:
   - Uses Kimi AI API for document analysis
   - System prompts stored in `src/main/resources/prompts/`
   - Streaming responses via Server-Sent Events (SSE)
   - Background processing for document classification

2. **File Processing**:
   - Apache POI for Word documents
   - PDFBox for PDF files
   - CommonMark for Markdown parsing
   - File storage in `./uploads/documents/`

3. **Real-time Features**:
   - SSE for streaming AI processing progress
   - Real-time status updates during document processing
   - Batch processing with individual file status tracking

4. **Data Flow**:
   - File upload → Content extraction → AI analysis → Reading guide generation → Classification tagging
   - All steps provide real-time feedback via SSE

## Database Schema

The system uses H2 database with automatic schema initialization via `src/main/resources/db.sql`. The main `literature` table includes:

- File metadata (name, path, size, type)
- Extracted content length
- AI-generated reading guide
- Classification tags (JSON array)
- Processing status
- Timestamps and soft delete flag

## Environment Requirements

### Prerequisites
- **Java 21+**: Required for Spring Boot 3.5.5 and modern Java features
- **Node.js 20+**: Required for Vue 3 frontend development
- **Maven 3.6+**: For backend dependency management and building

### Verification Commands
```bash
java -version    # Should show Java 21 or higher
node -v          # Should show Node.js 20 or higher
mvn -v           # Should show Maven 3.6 or higher
```

## Configuration Notes

- **Server runs on port 8086** with context path `/api`
- **API Documentation** available at `http://localhost:8086/api/doc.html`
- **Health Check** endpoint: `http://localhost:8086/api/literature/health`
- **File uploads** stored in `./uploads/documents/`
- **Database** persisted in `./data/` directory as H2 embedded database
- **Logs** written to `logs/literature-assistant.log`

### Key Configuration Sections

**File Upload Configuration** (`application.yml`):
```yaml
literature:
  file:
    upload-path: ./uploads/documents
    max-file-size: 10MB
    allowed-extensions: pdf,doc,docx,md,markdown
```

**AI Integration Configuration**:
```yaml
literature:
  ai:
    base-url: https://api.moonshot.cn/v1  # Kimi AI API
    model: kimi-k2-turbo-preview
    max-tokens: 20480
    temperature: 0.7
    timeout: 60000
```

**Database Configuration**:
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/literature_assistant  # Embedded H2
    driver-class-name: org.h2.Driver
```

## Development Patterns

- Uses Lombok for reducing boilerplate code
- MyBatis-Plus for enhanced ORM capabilities with XML mapping
- Global exception handling via `GlobalExceptionHandler`
- DTO pattern for request/response objects
- Builder pattern for complex API requests
- Virtual threads for background AI processing (non-blocking)
- CORS configuration for frontend integration
- Server-Sent Events (SSE) for real-time progress updates

## Database Schema

The system uses H2 database with automatic schema initialization via `src/main/resources/db.sql`. The main `literature` table includes:

- **id**: BIGINT AUTO_INCREMENT PRIMARY KEY
- **original_name**: VARCHAR(255) - Original uploaded filename
- **file_path**: VARCHAR(500) - Storage path on filesystem
- **file_size**: BIGINT - File size in bytes
- **file_type**: VARCHAR(10) - File extension/type
- **content_length**: INT - Extracted text content length
- **tags**: VARCHAR(2000) - AI-generated classification tags (JSON format)
- **description**: VARCHAR(2000) - AI-generated document description
- **reading_guide**: CLOB - AI-generated reading guide content
- **status**: TINYINT - Processing status (1=completed, 0=processing, etc.)
- **create_time**: TIMESTAMP - Record creation timestamp
- **update_time**: TIMESTAMP - Last update timestamp
- **deleted**: TINYINT - Soft delete flag (0=active, 1=deleted)

## Important Development Notes

### File Processing Pipeline
1. **Upload**: Files saved to `./uploads/documents/` with UUID naming
2. **Content Extraction**: Different parsers for PDF (PDFBox), Word (POI), Markdown (CommonMark)
3. **AI Analysis**: Kimi API processes extracted content for analysis
4. **Database Storage**: Results stored with tags and reading guides
5. **Real-time Updates**: SSE provides progress feedback throughout

### AI Integration Architecture
- **System Prompts**: Stored in `src/main/resources/prompts/`
- **Streaming**: Uses OkHttp with SSE support for real-time AI responses
- **Background Processing**: Virtual threads handle AI requests without blocking
- **Error Handling**: Comprehensive timeout and retry mechanisms

### Testing Strategy
- Minimal test coverage currently (only basic Spring Boot context test)
- Test structure exists in `src/test/java/`
- Consider adding unit tests for service layers and integration tests for API endpoints