# GSON-KSP

This is a ksp plugin to generate type adapter for your GSON model class.

Since most reflection operations at runtime are avoided, it is supposed to be faster than the normal GSON serializaiton/deserializaiton process.

Note this project is still working in progress 🚧.

## TODOs

- [x] Java deserialization code generation
- [x] Java serialization code generation
- [x] Java TypeAdapterFactory generation
- [x] Basic `@SerializedName` annotation support
- [ ] Kotlin code generation
- [ ] Code decoupling
- [ ] Replace switch-case branching with HashMap for better performance
- [ ] Field include/exclude rules support
- [ ] Code generation on demand
