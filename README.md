
Modify version of https://github.com/grails/grails-views/ with the main functionality kept.

Some features are not supported such as parent template, etc.

Use with your own risk. 


It has been published to maven central.

```xml
        <dependency>
            <groupId>io.github.aristotll</groupId>
            <artifactId>grails-jsonview-standalone</artifactId>
            <version>0.0.4</version>
        </dependency>

```

I just want to use grails json standalone without grails because it is too heavy for my project.

---

Not (yet) supported features

- tmpl. the template namespace todo
- groovy json render options
- Static Compilation


----



Won't support features

- parent template  
  - the parent template is not supported.  I don't want to support it because this makes the template hard to read.
- hal
- JSON API Support
- gorm model support
- Model Naming
- JSON View API


---- 

Additional supported features

Rails Jbuilder style syntax support

