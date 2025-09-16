# Proyecto de Documentación Automatizada de Cambios

Este proyecto permite generar documentación técnica de cambios en el código y procesos a partir de información de **Jira**, **Git** y **Confluence**, utilizando inteligencia artificial (IA) para estructurar el contenido en Markdown listo para QA y documentación interna.

---

## 🔹 Funcionalidad

1. **Obtención de datos desde Jira**:  
   Extrae el resumen y la descripción de una Historia de Usuario (HU) o ticket de Jira.

2. **Obtención de cambios desde Git**:  
   Recupera los commits de un branch específico que contengan la HU en su mensaje y lista los archivos modificados, indicando el tipo de cambio (modificado, nuevo, eliminado).

3. **Obtención de contenido desde Confluence**:  
   Extrae el contenido de páginas relacionadas para complementar la documentación.

4. **Generación de Markdown usando IA**:  
   Combina la información de Jira, Git y Confluence para generar documentación formal y técnica, siguiendo la estructura definida para QA:

   - Información General
   - Proceso
   - Garantías con Cámara
   - Complejidad (Alto, Medio, Bajo)
   - Menú
   - Autor
   - HU Relacionados
   - Descripción de Solución (Funcional y Técnica)
   - Criterios de Aceptación
   - Objetos Afectados (Java y Base de Datos)
   - Modelo Entidad Relación del Proceso

---

## 🔹 Requisitos

- **Java 17** o superior
- **Maven 3.8+**
- Acceso a:
  - Repositorio Git
  - Jira API
  - Confluence API
  - Vertex AI (Google) para generar contenido con IA
- Configuración de credenciales (Jira, Git, Confluence, Vertex AI)

---

## 🔹 Configuración

El proyecto utiliza `application.properties` o `application.yml` para manejar configuraciones y credenciales:

### Ejemplo `application.properties`

```properties
# Git
git.use.local=true
git.local.path=/ruta/al/repositorio
git.remote.url=https://github.com/usuario/repo.git
git.username=usuario
git.token=token_git

# Jira
jira.base.url=https://tujira.atlassian.net
jira.user=usuario@empresa.com
jira.api.token=token_jira

# Confluence
confluence.base.url=https://tuempresa.atlassian.net/wiki
confluence.user=usuario@empresa.com
confluence.token=token_confluence

# Vertex AI
spring.ai.vertex.ai.gemini.project-id=tu-proyecto
spring.ai.vertex.ai.gemini.location=us-central1
spring.ai.vertex.ai.gemini.chat.options.model=gemini-1.5-flash
gemini.api.key=tu_api_key

# Prompt personalizado
documentation.prompt=Plantilla de prompt para la IA con placeholders


## Uso
### 1. Ejecutar localmente

Clonar el repositorio:

git clone https://github.com/usuario/proyecto-documentacion.git
cd proyecto-documentacion


Construir el proyecto con Maven:

mvn clean install


Ejecutar la aplicación:

mvn spring-boot:run

### 2. Interfaz Web

Una vez en ejecución, la aplicación expone un formulario web para generar documentación:

URL: http://localhost:8080/docForm

Campos a completar:

Historia de Usuario (Jira ID)

Branch de Git

Proceso

Complejidad

Menú

Autor

HU Relacionados

El formulario obtiene automáticamente la información de Jira, Git y Confluence.

Al enviar, genera un Markdown completo listo para QA.

### 3. Uso desde código

Se puede invocar el servicio directamente:

@Autowired
private GeminiService geminiService;

String markdown = geminiService.generateMarkdown(
        jiraContent,
        gitChanges,
        confluenceContent,
        proceso,
        complejidad,
        menu,
        autor,
        huRelacionados
);

## Estructura del Proyecto
src/
 ├─ main/
 │   ├─ java/com/agent/sql/
 │   │   ├─ controller/      # Controladores web
 │   │   ├─ service/         # Interfaces de servicios
 │   │   ├─ impl/            # Implementaciones de servicios (Git, Jira, Confluence, IA)
 │   │   └─ util/            # Clases de utilidades
 │   └─ resources/
 │       ├─ application.properties
 │       └─ templates/       # HTML para formularios
 └─ test/                     # Pruebas unitarias

### Contribución

Hacer fork del proyecto

Crear branch con nueva funcionalidad o corrección

Abrir Pull Request explicando cambios

Revisar que los tests pasen antes de merge

### Licencia

Este proyecto está bajo licencia MIT.

### Notas

La aplicación no almacena credenciales en el código; todas se manejan por propiedades o variables de entorno.

La IA requiere conexión estable a Vertex AI; errores 503 indican problemas temporales de servicio.

Se recomienda usar un branch limpio para extraer commits de una HU específica