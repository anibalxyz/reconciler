# Reconciler

[🇬🇧 English Version](README.md)

Reconciler es una aplicación ligera y modular diseñada para ayudar a equipos a conciliar transacciones financieras entre extractos bancarios y sistemas internos. Construida siguiendo buenas prácticas de la industria, ofrece una plataforma intuitiva y personalizable con utilidades potentes tanto para usuarios individuales como para entornos colaborativos.

<details>
<summary>Tabla de Contenidos</summary>

- [Características](#características)
- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Getting Started](#getting-started)
- [Licencia](#licencia)

</details>

## Características

**Leyenda**:

- ✅ **Done** - Característica implementada y funcional
- 🔨 **Done+** - Característica implementada con oportunidades de mejora conocidas
- 🚧 **Doing** - Actualmente en desarrollo
- 📋 **Todo** - Planificada para implementación futura

---

- 🚧 **Sitio Público**: Sitio de acceso general con portal de inicio de sesión/registro usando Astro para SSR optimizado para SEO
  - *Estado actual*: Infraestructura técnica completa, contenido placeholder temporal y navbar
- 🔨 **Autenticación de Usuario**: Autenticación basada en JWT con rotación de refresh tokens, control de acceso por ventana de tiempo (Lun-Vie 08:00-20:00), y funcionalidad de cierre de sesión
  - *Mejoras conocidas*: Reglas adicionales de ventana de tiempo, mecanismos de bloqueo de cookies revocadas
- 🚧 **Panel de Control**: Centro principal basado en React para navegar por las características de la aplicación
  - *Estado actual*: Flujo de autenticación completo, enlace temporal a Swagger UI como placeholder
- 🔨 **Gestión de Usuarios**: Operaciones CRUD para cuentas de usuario con control de acceso basado en roles
  - *Mejoras conocidas*: Paginación, restringir creación solo a administradores
- ✅ **Documentación de API**: Swagger UI interactivo con especificaciones OpenAPI para todos los endpoints
- 📋 **Configuración Inicial**: Configurar parámetros centrales como fuentes de transacciones, categorías y acciones de registro disponibles (requerido una vez después de la implementación)
- 📋 **Carga de Transacciones**: Cargar datos tanto del banco como de los sistemas internos en formatos compatibles
- 📋 **Conciliación Automatizada**: Identifica automáticamente coincidencias y discrepancias entre las transacciones cargadas
- 📋 **Resolución Manual de Discrepancias**: Interfaz para resolver registros no coincidentes o ambiguos
- 📋 **Panel de Administración**: Sección solo para administradores para definir nuevos parámetros del sistema (por ejemplo, categorías, fuentes, tipos de acción)
- 📋 **Informes y Exportaciones**: Exportar datos en formatos PDF, Excel u otros formatos convencionales
- 📋 **Análisis y Gráficos**: Visualizar la actividad financiera, las tasas de conciliación y las tendencias

## Stack Tecnológico

- **Backend**: Java 21 con Javalin
- **Frontend**: TypeScript + TailwindCSS
  - **Dashboard**: Vite + React
  - **Sitio Público**: Astro
- **Base de Datos**: PostgreSQL con Flyway para migraciones
- **Servidor Web**: Nginx (producción)
- **CLI**: Python 3 con Typer
- **Contenedorización**: Docker & Docker Compose

## Estructura del Proyecto

Resumen de los archivos y directorios más importantes:

```text
.
├── cli/                 # Herramienta CLI de Python
│   ├── src/             # Código fuente
│   │   └── modules/     # Módulos de comandos de la CLI
│   └── pyproject.toml   # Definición del proyecto y dependencias
├── backend/
│   ├── api/             # Código fuente Java (Javalin)
│   │   └── pom.xml      # Dependencias del Backend (Maven)
│   └── db/
│       └── migrations/  # Migraciones de la base de datos (Flyway)
├── frontend/
│   ├── common/          # Utilidades y servicios compartidos
│   ├── dashboard/       # Aplicación React para el dashboard
│   └── public-site/     # Aplicación Astro para el sitio público
├── nginx/               # Configuración de Nginx para producción
├── compose.yaml         # Configuración base de Docker Compose para todos los servicios
├── compose.<env>.yaml   # Overrides de Docker Compose para el entorno <env>
└── README.es.md         # Este archivo
```

## Getting Started

### Prerrequisitos

- **Git** (para clonar el repositorio)
- **Docker v20.10+** y Docker Compose v2+ (para ejecutar la aplicación). **No usar v5.x**
- **Python 3.8+** y **pip** (para usar la CLI)

> [!WARNING]
> Este proyecto está diseñado para ejecutarse con Docker; es el enfoque recomendado y oficialmente soportado. Ejecutar servicios localmente en la máquina host es parcialmente soportado para **API y frontend** (principalmente para desarrollo), pero puede requerir configuración manual adicional.
>
> **Para desarrollo local** (API y frontend):
>
> - **Java 21** y **Maven**: Para compilar y ejecutar la API de backend
> - **Node.js 22+** y un gestor de paquetes (`npm`, `pnpm` o `yarn`): Para compilar y ejecutar las aplicaciones frontend
> - **PostgreSQL Server**: Instancia en ejecución para que la aplicación se conecte
>
> **No soportado para ejecución local** (servicios que deben correr vía Docker):
>
> - **Flyway**: Las migraciones de base de datos deben ejecutarse vía Docker
> - **Nginx**: La configuración del proxy inverso para producción es Docker-only
>
> El soporte para ejecutar todos los servicios localmente no está planificado por ahora.

### 1. Clonar el repositorio

```bash
git clone https://github.com/anibalxyz/reconciler.git

# Los siguientes comandos asumirán que estás en la raíz del proyecto
cd reconciler
```

### 2. Instalar la CLI

La gestión del proyecto se realiza mediante una CLI personalizada. Se recomienda instalarla en un entorno virtual.

```bash
# Crear y activar un entorno virtual
python3 -m venv ./cli/.venv
source ./cli/.venv/bin/activate

# Instalar la CLI en modo editable
pip install -e ./cli[dev]
```

```bash
# Si no quieres usar modo editable
pip install ./cli
```

Gracias a [Typer](https://typer.tiangolo.com/), la CLI está completamente auto-documentada, por lo que puedes obtener
ayuda para cualquier comando o subcomando simplemente agregando `--help`.

```bash
# Prueba
cli --help
```

### 3. Configurar el entorno

La CLI gestiona entornos (`dev`, `prod`, `test`, etc.). Selecciona el entorno e inicializa la configuración con `--init`.

```bash
# Sintaxis: cli set env <environment> --init
cli set env dev --init
```

Este comando:

1. Persiste el entorno elegido en `cli.cfg`.
2. Si faltan, crea archivos `.env.*` a partir de las plantillas `.example` y abrirá `nano` para editarlos.

### 4. Ejecutar la aplicación

Primero, construir las imágenes Docker para todos los servicios del entorno actual.

```bash
cli image build all
```

Luego, iniciar los servicios con Docker Compose.

```bash
cli compose up all
```

### 5. Detener la aplicación

Para detener y eliminar contenedores y redes en ejecución:

```bash
cli compose down all
```

### Acceso a la aplicación

Después de ejecutar `compose up`, puede acceder a los servicios en las siguientes URL.

> [!NOTE]
> Los puertos que se enumeran a continuación son los valores predeterminados definidos en los archivos `.env`. Si los cambia, deberá ajustar las URL en consecuencia.

| Entorno | Servicio           | URL                                              | Descripción                             |
| :------ | :----------------- | :----------------------------------------------- | :-------------------------------------- |
| `dev`   | API                | <http://localhost:4001/>                         | Swagger UI para documentación de la API |
| `dev`   | Sitio Público      | <http://localhost:5174/>                         | Páginas de login y registro             |
| `dev`   | Dashboard          | <http://localhost:5175/>                         | Dashboard autenticado (requiere login)  |
| `prod`  | Frontend via Nginx | <http://localhost/>                              | público y dashboard                     |
| `prod`  | API via Nginx      | <http://localhost/api/>                          | API y Swagger UI                        |

> [!TIP]
> Para acceder al Dashboard debes iniciar sesión primero desde el Sitio Público. El Dashboard incluye un enlace temporal a Swagger UI para explorar la API.

## Licencia

Este proyecto está bajo la Licencia MIT. Consulte el archivo [LICENSE](LICENSE) para más detalles.
