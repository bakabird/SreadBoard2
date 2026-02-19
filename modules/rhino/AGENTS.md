# RHINO MODULE GUIDE

## OVERVIEW

`modules/rhino` wraps Mozilla Rhino and exposes a script-engine style API used by app runtime features.

## STRUCTURE

```text
modules/rhino/
|- src/main/java/com/script/
|  |- rhino/  # Rhino wrappers, context, adapters, security shutters
|  |- *.kt    # script-engine interfaces and bindings
|- lib/rhino-1.7.14.jar
|- consumer-rules.pro
```

## WHERE TO LOOK

- Engine entry: `modules/rhino/src/main/java/com/script/rhino/RhinoScriptEngine.kt`
- Context/wrap factories: `modules/rhino/src/main/java/com/script/rhino/RhinoContext.kt`, `modules/rhino/src/main/java/com/script/rhino/RhinoWrapFactory.kt`
- Access controls: `modules/rhino/src/main/java/com/script/rhino/RhinoClassShutter.kt`, `modules/rhino/src/main/java/com/script/rhino/ReadOnlyJavaObject.kt`
- Bundled runtime: `modules/rhino/lib/rhino-1.7.14.jar`

## CONVENTIONS

- Preserve compatibility at scripting boundary (`ScriptEngine`, `Bindings`, `Invocable` behaviors).
- Keep wrapper/access-control semantics stable (class shutter/read-only object policies).
- Treat bundled Rhino runtime version as deliberate and explicit.

## ANTI-PATTERNS

- Do not remove or alter Rhino copyright headers in wrapped source files.
- Do not bypass class-shutter/read-only wrappers when exposing host objects to scripts.
