# Reconciler

[🇬🇧 English Version](README.md)

Reconciler es una aplicación ligera y modular diseñada para ayudar a los equipos a conciliar transacciones financieras
entre extractos bancarios y sistemas internos. Construida con las mejores prácticas de la industria, tiene como objetivo
proporcionar una plataforma intuitiva y personalizable con potentes utilidades tanto para usuarios individuales como
para entornos colaborativos.

<details>
<summary>Tabla de Contenidos</summary>

- [Características](#características)
- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Prerrequisitos](#prerrequisitos)
- [Primeros Pasos](#primeros-pasos)
- [Acceso a la Aplicación](#acceso-a-la-aplicación)
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
  - **Panel de Control**: Vite + React
  - **Sitio Público**: Astro
- **Base de Datos**: PostgreSQL con Flyway para migraciones
- **Servidor Web**: Nginx (para producción)
- **CLI**: Python 3 con Typer
- **Contenedorización**: Docker y Docker Compose

## Estructura del Proyecto

Una breve descripción de los archivos y directorios más importantes del proyecto:

```text
.
├── cli/                 # Herramienta CLI de Python
│   ├── src/             # Código fuente
│   │   └── modules/     # Módulos de comandos de la CLI
│   └── pyproject.toml   # Definición del proyecto y dependencias
├── backend/
│   ├── api/             # Código fuente de la API de Java (Javalin)
│   │   └── pom.xml      # Dependencias del Backend (Maven)
│   └── db/
│       └── migrations/  # Migraciones de la base de datos (Flyway)
├── frontend/
│   ├── common/          # Utilidades y servicios compartidos
│   ├── dashboard/       # Aplicación React para el panel de control
│   └── public-site/     # Aplicación Astro para el sitio público
├── nginx/               # Configuración de Nginx para el entorno de producción
├── compose.yaml         # Configuración base de Docker Compose para todos los servicios
├── compose.<env>.yaml   # Sobrescrituras de Docker Compose para el entorno <env>
└── README.es.md         # Este archivo
```

## Prerrequisitos

- **Git** (para clonar el repositorio).
- **Docker v20.10+** y Docker Compose v2+ (para ejecutar la aplicación). **No usar v5.x**.
- **Python 3.8+** y **pip** (para usar la herramienta CLI).

> [!WARNING]
> Este proyecto está diseñado para ejecutarse con Docker, que es el enfoque recomendado y oficialmente compatible. Ejecutar los servicios localmente en su máquina host es parcialmente compatible para **API y frontend** (principalmente para desarrollo), pero puede requerir configuración manual adicional.
>
> **Para desarrollo local** (servicios de API y frontend):
>
> - **Java 21** y **Maven**: Para construir y ejecutar la API de backend
> - **Node.js 22+** y un administrador de paquetes (`npm`, `pnpm`, o `yarn`): Para construir y ejecutar las aplicaciones frontend
> - **Servidor PostgreSQL**: Una instancia en ejecución para que la aplicación se conecte
>
> **No compatible para ejecución local** (servicios solo Docker):
>
> - **Flyway**: Las migraciones de base de datos deben ejecutarse vía Docker
> - **Nginx**: La configuración del proxy inverso de producción es solo Docker
>
> El soporte de ejecución local para todos los servicios no está planificado hasta que el proyecto alcance un estado maduro.

## Primeros Pasos

### 1. Clonar el Repositorio

```bash
git clone https://github.com/anibalxyz/reconciler.git
cd reconciler
```

### 2. Instalar la CLI

Este proyecto es administrado por una potente interfaz de línea de comandos personalizada. La CLI proporciona una forma
unificada e intuitiva de administrar los entornos y el ciclo de vida de la aplicación. Tiene varias características,
¡pero ya lo veremos en acción más abajo!

La CLI es una aplicación Python. Se recomienda instalarla en un entorno virtual.

```bash
# Crear y activar un entorno virtual (opcional pero recomendado)
python3 -m venv ./cli/.venv
source ./cli/.venv/bin/activate

# Instalar la CLI en modo editable
pip install -e ./cli[dev]
```

```bash
# Si no desea usar el modo editable
pip install ./cli
```

Gracias a [Typer](https://typer.tiangolo.com/), la CLI está completamente auto-documentada, por lo que puede obtener
ayuda para cualquier comando o subcomando simplemente agregando `--help`.

```bash
# ¡Pruébalo!
cli --help
```

### 3. Configurar el Entorno

La CLI puede administrar diferentes entornos (por ejemplo, `dev`, `prod`, `test`). Configure su entorno deseado e
inicialice los archivos de configuración de una sola vez usando la bandera `--init`. Esta es la forma recomendada de
comenzar.

```bash
# Sintaxis: cli set env <environment> --init
cli set env dev --init
```

Este comando:

1. Persistirá el entorno elegido en el archivo `cli.cfg`.
2. Si no existen, creará archivos `.env.*` a partir de sus plantillas `.example` y le pedirá que los edite con `nano`.

### 4. Ejecutar la Aplicación

Primero, construya las imágenes de Docker para todos los servicios en el entorno actual.

```bash
cli image build all
```

Luego, inicie los servicios usando Docker Compose.

```bash
cli compose up all
```

### 5. Detener la Aplicación

Para detener y eliminar todos los contenedores y redes en ejecución, use `compose down`.

```bash
cli compose down all
```

## Acceso a la Aplicación

Después de ejecutar `compose up`, puede acceder a los servicios en las siguientes URL.

> [!NOTE]
> Los puertos que se enumeran a continuación son los valores predeterminados definidos en los archivos `.env`. Si los cambia, deberá ajustar las URL en consecuencia.

| Entorno | Servicio                   | URL                      | Descripción                                     |
|:--------|:---------------------------|:-------------------------|:------------------------------------------------|
| `dev`   | API                        | <http://localhost:4001/> | Swagger UI para documentación de la API         |
| `dev`   | Sitio Público              | <http://localhost:5174/> | Páginas de inicio de sesión y registro          |
| `dev`   | Panel de Control           | <http://localhost:5175/> | Panel de control autenticado (requiere login)   |
| `prod`  | Frontend a través de Nginx | <http://localhost/>      | Sitio público y panel de control                |
| `prod`  | API a través de Nginx      | <http://localhost/api/>  | API y Swagger UI                                |

> [!TIP]
> Para acceder al Panel de Control, primero deberá iniciar sesión a través del Sitio Público. Hay un enlace temporal a Swagger UI disponible en el Panel de Control para la exploración de la API.

## Licencia

Este proyecto está bajo la Licencia MIT. Consulte el archivo [LICENSE](LICENSE) para obtener más detalles.
