# Liverpool – Examen técnico QA Automation

Automatización con **Selenium WebDriver + Java 17 + TestNG + Maven** del flujo de compra de
[liverpool.com.mx](https://www.liverpool.com.mx/tienda/home):

> Con sesión iniciada → navegar productos (Botas, Mocasines, Perfumes, Computadora Lenovo, Pantalla Sony, Xbox)
> → agregarlos a la bolsa → **Ir a comprar ahora**.

El flujo **se detiene en el checkout**: nunca se paga ni se generan pedidos reales.

---

## 1. Estructura

```
src/main/java/com/liverpool/qa
├── config/    Config.java            URLs, credenciales y timeouts (variables de entorno)
├── driver/    DriverFactory.java     ChromeDriver por hilo (Selenium Manager descarga el driver)
├── model/     Category.java          Los 6 productos del examen (ruta de menú + término de búsqueda)
├── pages/     Page Objects           BasePage, LoginPage, HomePage, ProductListPage,
│                                     ProductDetailPage, CartPage, CheckoutPage
└── flows/     ShoppingFlow.java      Flujos reutilizables (sin aserciones)

src/test/java/com/liverpool/qa
├── tests/     BaseTest, LoggedInBaseTest, ShoppingFlowTests, GuestNegativeTests,
│              CartNegativeTests, EdgeCaseTests
└── listeners/ TestListener.java      Captura de pantalla al fallar (target/screenshots)
```

## 2. Decisiones de diseño

- **Page Object Model**: los page objects exponen acciones y estados; **las aserciones viven en los tests**, no en las páginas.
- **Solo waits explícitos** (`WebDriverWait` / `ExpectedConditions`). Sin `Thread.sleep` ni implicit wait (mezclarlos produce esperas impredecibles).
- **Click robusto** (`BasePage.click`): reintenta ante `StaleElementReferenceException` y, si un banner/modal intercepta el click, intenta cerrar overlays y reintenta.
- **Navegación por menú con respaldo**: el examen pide "navegar", así que primero se intenta el menú; si el menú cambió, se usa el buscador del sitio. Para Lenovo/Sony/Xbox se busca directo (son producto + marca, no una categoría del menú).
- **Producto "disponible"**: los primeros resultados pueden estar agotados, por lo que se prueban hasta 5 candidatos y se toma el primero con botón de agregar habilitado.
- **Conteo de la bolsa por botones "Eliminar"**: cada línea de la bolsa tiene uno; los links a productos no sirven porque la bolsa muestra carruseles de recomendados.
- **Estado de la bolsa**: vive en la cuenta (servidor), no en el navegador. Cada test que depende de ella la limpia primero y los tests **no se paralelizan**.
- **Credenciales** solo por variables de entorno; nunca en el repositorio.

## 3. Requisitos y ejecución

- JDK 17+, Maven 3.8+, Google Chrome instalado.
- Una **cuenta de pruebas** de Liverpool (no uses tu cuenta personal).

```bash
# Linux / macOS
export LIVERPOOL_USER="tu_correo@ejemplo.com"
export LIVERPOOL_PASSWORD="tu_contraseña"
mvn clean test

# Windows (PowerShell)
$env:LIVERPOOL_USER="tu_correo@ejemplo.com"
$env:LIVERPOOL_PASSWORD="tu_contraseña"
mvn clean test
```

### Sesión iniciada con cookies (en lugar de usuario y contraseña)

Útil si el login tiene captcha o 2FA. Los tests con sesión usan las cookies si están configuradas; si no, hacen login con usuario y contraseña.

1. Inicia sesión manualmente en liverpool.com.mx con tu cuenta de pruebas.
2. Exporta las cookies del dominio a JSON (por ejemplo con la extensión *Cookie-Editor* → Export → JSON) y guárdalas como `cookies.json` en la raíz del proyecto (ya está en `.gitignore`).
3. Corre con:

```bash
export COOKIES_FILE=cookies.json        # PowerShell: $env:COOKIES_FILE="cookies.json"
mvn clean test
```

En CI puedes pasar el mismo JSON como secreto en `COOKIES_JSON`.

- Formato: arreglo de objetos con `name` y `value` (opcionales `domain`, `path`, `secure`, `httpOnly`, `expiry` o `expirationDate`).
- Las cookies de sesión **expiran**: si el test falla con "el sitio sigue sin sesión", exporta cookies nuevas.
- **Nunca subas las cookies al repositorio**: equivalen a tu contraseña mientras la sesión esté activa.

| Variable | Default | Uso |
|---|---|---|
| `COOKIES_FILE` / `COOKIES_JSON` | – | Cookies de una sesión iniciada (tienen prioridad sobre el login) |
| `LIVERPOOL_USER` / `LIVERPOOL_PASSWORD` | – | Cuenta de pruebas. Sin cookies ni credenciales, los tests con sesión se **omiten** (skip) |
| `HEADLESS` | `false` | `true` para correr sin ventana |
| `USE_MENU_NAVIGATION` | `true` | `false` fuerza navegación solo por búsqueda |
| `TIMEOUT_SECONDS` | `20` | Timeout de waits explícitos |
| `BASE_URL`, `LOGIN_URL`, `CART_URL` | URLs de producción | Útil si existe un ambiente de QA |

Reportes: `target/surefire-reports/` · Capturas de fallos: `target/screenshots/`.

> ⚠️ **Nota importante:** los *locators* (`By`) están escritos con criterios estables (texto, atributos, `href`)
> pero deben **validarse contra el sitio real** en la primera corrida. Los puntos a revisar están marcados con
> `VALIDAR` en los Page Objects.

---

## 4. Casos de prueba

### 4.1 Funcionales
| ID | Caso | Resultado esperado |
|---|---|---|
| FUNC-01 | Navegar a cada uno de los 6 productos/categorías (data provider) | Se listan productos |
| FUNC-02 | Agregar un producto disponible de cada categoría a la bolsa y presionar **Ir a comprar ahora** | La bolsa tiene 6 líneas y se llega al checkout |

### 4.2 Negativos
| ID | Caso | Resultado esperado |
|---|---|---|
| NEG-01 | Login con credenciales inválidas | Mensaje de error, sin sesión |
| NEG-02 | Búsqueda sin coincidencias | Estado vacío, sin productos |
| NEG-03 | Agregar zapatos sin elegir talla | No se agrega a la bolsa |
| NEG-04 | Bolsa vacía | Mensaje de vacío y sin botón de compra |

### 4.3 Edge cases
| ID | Caso | Resultado esperado |
|---|---|---|
| EDGE-01 | Búsqueda vacía | Sin error ni página rota |
| EDGE-02 | Caracteres especiales / `<script>` en el buscador | No se ejecuta el script, sin error |
| EDGE-03 | Término de 500 caracteres | Se maneja sin error |
| EDGE-04 | Agregar dos veces el mismo producto | Suma cantidad o duplica línea; sigue comprable |
| EDGE-05 | Recargar / navegar y volver a la bolsa | La bolsa persiste idéntica |

**Casos identificados y no automatizados** (no deterministas o con riesgo de efectos reales):
producto agotado/sin stock, cambio de precio entre PDP y bolsa, límite de cantidad por producto, cupones y
promociones, expiración de sesión durante el flujo, pago real, múltiples pestañas/sesiones simultáneas.

## 5. Riesgos

| # | Riesgo | Mitigación |
|---|---|---|
| 1 | **Locators frágiles**: el sitio es una SPA con clases generadas que cambian entre despliegues | Locators por texto/atributos/`href`, centralizados en cada Page Object |
| 2 | **Protección anti-bot / captcha / rate limiting** puede bloquear la automatización | Ejecutar en un ambiente de QA o con allow-list; no se evade la protección |
| 3 | **Login con 2FA, captcha o verificación por correo** impide un login automático | Cuenta de pruebas sin 2FA, o reutilizar una sesión/cookies preautenticadas |
| 4 | **Datos dinámicos**: stock, precio, ofertas y orden de resultados cambian a diario | Se prueban hasta 5 candidatos y se toma el primero disponible; sin precios hardcodeados |
| 5 | **Modales y banners** (cookies, ubicación/CP, promociones) interceptan clicks | `dismissOverlays()` + click con reintento |
| 6 | **Estado compartido de la bolsa** en la cuenta: otra ejecución o persona la altera | Limpieza previa, cuenta dedicada, sin ejecución paralela |
| 7 | **Compras reales** accidentales | El flujo se detiene en el checkout, no se paga |
| 8 | **Producción como ambiente de pruebas**: carga, datos reales y términos de uso | Recomendado un ambiente de QA; volumen bajo de ejecución |
| 9 | **Variantes obligatorias** (talla, color, garantía) distintas por producto | `selectFirstAvailableOption()` y caso negativo NEG-03 |
| 10 | **Elementos duplicados** por diseño responsivo (versión móvil/escritorio) | Se filtran elementos no visibles; viewport fijo 1920×1080 |
| 11 | **Credenciales y datos personales** | Variables de entorno, `.gitignore`, cuenta de pruebas |
| 12 | **Flakiness** por tiempos de carga de la SPA | Solo waits explícitos y reintentos en acciones críticas |

## 6. Mejoras futuras
Integración con GitHub Actions, reporte Allure, ejecución cross-browser (Firefox/Edge), reutilización de la sesión
(cookies) para evitar el login en cada clase, y ambiente de QA dedicado.
