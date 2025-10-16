[English Version](README.md)

# Reconciler

Reconciler es una aplicación ligera y modular diseñada para ayudar a los equipos a conciliar transacciones financieras
entre extractos bancarios y sistemas internos. Construida con las mejores prácticas de la industria, tiene como objetivo
proporcionar una plataforma intuitiva y personalizable con potentes utilidades tanto para usuarios individuales como
para entornos colaborativos.

<details>
<summary>Tabla de Contenidos</summary>

- [Características Planificadas](#características-planificadas)
- [Stack Tecnológico](#stack-tecnológico)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Prerrequisitos](#prerrequisitos)
- [Primeros Pasos](#primeros-pasos)
- [Uso de la CLI](#uso-de-la-cli)
- [Acceso a la Aplicación](#acceso-a-la-aplicación)
- [Licencia](#licencia)

</details>

## Características Planificadas

- **Configuración Inicial**: Configurar parámetros centrales como fuentes de transacciones, categorías y acciones de
  registro disponibles. Este paso solo se requiere una vez después de la implementación.
- **Autenticación de Usuario**: Sistema de inicio de sesión básico utilizando credenciales predefinidas. Los usuarios
  pueden actualizar su contraseña más tarde, pero solo los administradores pueden crear o administrar cuentas.
- **Panel de Control**: Centro principal para navegar por las características de la aplicación.
- **Carga de Transacciones**: Cargar datos tanto del banco como de los sistemas internos en formatos compatibles.
- **Conciliación Automatizada**: Identifica automáticamente coincidencias y discrepancias entre las transacciones
  cargadas.
- **Resolución Manual de Discrepancias**: Interfaz para resolver registros no coincidentes o ambiguos.
- **Panel de Administración**: Sección solo para administradores para agregar usuarios y definir nuevos parámetros del
  sistema (por ejemplo, categorías, fuentes, tipos de acción).
- **Informes y Exportaciones**: Exportar datos en formatos PDF, Excel u otros formatos convencionales.
- **Análisis y Gráficos**: Visualizar la actividad financiera, las tasas de conciliación y las tendencias.
- **Sitio Público**: Sitio de acceso general con información del producto y portal de inicio de sesión/registro.

## Stack Tecnológico

- **Backend**: Java 21 con Javalin
- **Frontend**: TypeScript con Vite
    - **Panel de Control**: React
    - **Sitio Público**: TypeScript y HTML puro
- **Base de Datos**: PostgreSQL con Flyway para migraciones
- **Servidor Web**: Nginx (para producción)
- **CLI**: Python 3 con Typer
- **Contenerización**: Docker y Docker Compose

## Estructura del Proyecto

Una breve descripción de los archivos y directorios más importantes del proyecto:

```
.
├── cli/                 # Código fuente de la herramienta CLI de Python
│   └── main.py          # Punto de entrada para la CLI
├── backend/
│   ├── api/             # Código fuente de la API de Java (Javalin)
│   │   └── pom.xml      # Dependencias del Backend (Maven)
│   └── db/
│       └── migrations/  # Migraciones de la base de datos (Flyway)
├── frontend/
│   ├── dashboard/       # Aplicación React para el panel de control
│   └── public-site/     # Aplicación TypeScript para el sitio público
├── nginx/               # Configuración de Nginx para el entorno de producción
├── compose.yaml         # Configuración base de Docker Compose para todos los servicios
├── compose.<env>.yaml   # Sobrescrituras de Docker Compose para el entorno <env>
├── pyproject.toml       # Definición del proyecto y dependencias para la herramienta CLI
└── README.md            # Este archivo
```

## Prerrequisitos

- **Git** (para clonar el repositorio).
- **Docker v20.10+** y Docker Compose v2+ (para ejecutar la aplicación).
- **Python 3.8+** y **pip** (para usar la herramienta CLI).

> [!WARNING]
> Este proyecto está diseñado para ejecutarse con Docker, que es el enfoque recomendado. Ejecutar los servicios
> localmente en su máquina host no es oficialmente compatible y puede provocar errores inesperados o requerir
> configuración manual adicional. Si desea ejecutar los servicios localmente, deberá instalar y configurar lo siguiente:
>
> - **Java 21** y **Maven**: Para construir y ejecutar la API de backend.
> - **Node.js 22+** y un administrador de paquetes (e.g. `npm`): Para construir y ejecutar las aplicaciones frontend.
> - **Servidor PostgreSQL**: Una instancia en ejecución para que la aplicación se conecte.
> - **CLI de Flyway**: Para ejecutar migraciones de base de datos en su instancia local de PostgreSQL.
> - **Nginx**: Para replicar la configuración del proxy inverso del entorno de producción.

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
pip install -e .[dev]
```

```bash
# Si no desea usar el modo editable
pip install .
```

Gracias a [Typer](https://typer.tiangolo.com/), la CLI está completamente autodocumentada, por lo que puede obtener
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

> [!TIP]
> La única interfaz "bonita" disponible de forma predeterminada es la **Swagger UI** para la API, que proporciona una
> documentación completa e interactiva para todos los endpoints de la API. Las aplicaciones frontend (Panel de Control y
> Sitio Público) aún no están completamente inicializadas, pero se puede acceder a ellas si desea ver su estado actual.

> [!NOTE]
> Los puertos que se enumeran a continuación son los valores predeterminados definidos en los archivos `.env`. Si los
> cambia, deberá ajustar las URL en consecuencia.

| Entorno | Servicio                   | URL                    |
|:--------|:---------------------------|:-----------------------|
| `dev`   | API                        | http://localhost:4001/ |
| `dev`   | Sitio Público              | http://localhost:5173/ |
| `dev`   | Panel de Control           | http://localhost:5174/ |
| `prod`  | Frontend a través de Nginx | http://localhost/      |
| `prod`  | API a través de Nginx      | http://localhost/api/  |

## Licencia

Este proyecto está bajo la Licencia MIT. Consulte el archivo [LICENSE](LICENSE) para obtener más detalles.
