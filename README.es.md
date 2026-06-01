# Reconciler

[🇬🇧 English Version](/README.md)

Una plataforma de conciliación de transacciones. Construida desde cero como proyecto personal para **aprender** ingeniería de software moderna y buenas prácticas.

> [!NOTE]
> Esta traducción fue generada mayormente de forma automática, por lo que puede no sonar natural en algunas partes.

<details>
<summary>Tabla de Contenidos</summary>

- [Overview](#overview)
  - [Product](#product)
  - [Personal Project](#personal-project)
- [Características](#características)
- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Getting Started](#getting-started)
- [Licencia](#licencia)

</details>

## Overview

### Product

Una plataforma de conciliación financiera para emparejar transacciones entre extractos bancarios y sistemas internos. Cargá datos de ambas fuentes, deja que el sistema encuentre coincidencias, luego revisá y resolvé discrepancias.

Esta es la cara visible del proyecto y lo que finalmente entregará. Pensalo como la excusa que justifica construir todo lo que está debajo.

### Personal Project

Reconciler empezó como un simple CLI en Java para aprender sintaxis de Java (y POO a la fuerza) antes de arrancar un proyecto más grande que tenía planeado.

Pero se me ocurrió agregarle una UI web, luego agregar Docker para aprender en el camino, luego implementar cosas que iba aprendiendo en paralelo como buenas prácticas,
luego me di cuenta de que podía automatizar el setup de Docker con una CLI custom (inicialmente hecha con GNU Make), y después... acá estamos: esto se convirtió en el 99% de mi portafolio,
un sistema de nivel productivo (lo dijo Claude, así que debe ser cierto) que construí de cero hasta este punto.

Estoy aplicando clean architecture con feature-based packaging, testing comprehensivo (unit, integration, coverage), Docker con multi-environment Compose, CI/CD con GitHub Actions, observabilidad con Prometheus, Grafana, Loki y Promtail, y gestión de infraestructura desde la CLI hasta el servidor deployado.

Por supuesto que queda un largo camino por recorrer (que se alarga cada día que aprendo algo nuevo), pero estoy orgulloso de cuánto he crecido y aprendido; me siento listo y _confiado_ para integrarme a un equipo y contribuir desde el día uno.

Hay una cantidad asombrosa e increíble de _cero_ "Características de Producto"... pero hay una base suficientemente sólida lista para crecer y construir algo real

## Características

**Leyenda**:

- ✅ **Done** - Característica implementada y funcional
- 🔨 **Done+** - Característica implementada con oportunidades de mejora conocidas
- 🚧 **Doing** - Actualmente en desarrollo
- 📋 **Todo** - Planificada para implementación futura

---

- 🚧 **Sitio Público**: Sitio de acceso general con portal de inicio de sesión/registro usando Astro para SSR optimizado para SEO
  - _Estado actual_: Infraestructura técnica completa, contenido placeholder temporal y navbar
- 🔨 **Autenticación de Usuario**: Autenticación basada en JWT con rotación de refresh tokens, control de acceso por ventana de tiempo (Lun-Vie 08:00-20:00), y funcionalidad de cierre de sesión
  - _Mejoras conocidas_: Reglas adicionales de ventana de tiempo, mecanismos de bloqueo de cookies revocadas
- 🚧 **Panel de Control**: Centro principal basado en React para navegar por las características de la aplicación
  - _Estado actual_: Flujo de autenticación completo, enlace temporal a Swagger UI como placeholder
- 🔨 **Gestión de Usuarios**: Operaciones CRUD para cuentas de usuario con control de acceso basado en roles
  - _Mejoras conocidas_: Paginación, restringir creación solo a administradores
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
├── backend/
│   ├── api/             # Código fuente Java (Javalin)
│   └── db/
│       └── migrations/  # Migraciones de base de datos (Flyway)
├── frontend/
│   ├── common/          # Utilidades y servicios compartidos
│   ├── dashboard/       # Aplicación React para el dashboard
│   └── public-site/     # Aplicación Astro para el sitio público
├── nginx/               # Configuración de Nginx para producción
├── monitoring/          # Configuración del stack de monitoreo (Prometheus, Grafana, Loki, Promtail)
├── compose.yaml         # Configuración base de Docker Compose para todos los servicios
├── compose.<env>.yaml   # Overrides de Docker Compose para el entorno <env>
├── docs/                # Documentación de referencia (backend, frontend, infra, cli)
├── .agents/skills/      # Convenciones procedurales para commits, PRs, testing, releases
├── .github/             # Templates de issues y PRs, workflows CI/CD
├── AGENTS.md            # Guía para agentes de IA
├── CONTRIBUTING.md      # Guía de contribución
├── README.md            # Este archivo (inglés)
└── README.es.md         # Este archivo
```

## Getting Started

**Requisitos previos:** Git, Docker v20.10+ con Compose v2+ o v5.1+, Python 3.10+.

> [!NOTE]
> Docker es la forma activamente soportada de ejecutar el proyecto.
> Algunos servicios también pueden ejecutarse localmente para desarrollo, pero requieren herramientas adicionales.

```bash
# Clonar el repositorio
git clone git@github.com:anibalxyz/reconciler.git
cd reconciler
```

```bash
# Configurar el entorno e inicializar archivos de configuración desde plantillas
# Se te pedirá completar los valores en `.env.dev` usando `nano`
cli set env dev --init
```

```bash
# Construir todas las imágenes Docker necesarias
cli image build all
```

```bash
# Iniciar todos los servicios
cli compose up all
```

| Servicio      | URL (puerto por defecto) |
| :------------ | :----------------------- |
| API           | <http://localhost:4001/> |
| Sitio Público | <http://localhost:5174/> |
| Dashboard     | <http://localhost:5175/> |

```bash
# Detener y eliminar todos los contenedores
cli compose down all
```

> [!TIP]
> Para la guía completa incluyendo prerrequisitos, instalación de la CLI, dev walkthrough y arquitectura, ver [docs/infra/cli.md](docs/infra/cli.md).
>
> Un comando demo de una línea con configuración fija (sin necesidad de setup) estará disponible pronto.

## Licencia

Este proyecto está bajo la Licencia MIT. Consulte el archivo [LICENSE](/LICENSE) para más detalles.
