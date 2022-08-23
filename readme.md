# Modular matrix inverse with Z3

### With IDEA
There are my IntelliJ IDEA Settings. Update them according your working environment.
```
Main class: me.vzhilin.matrix.Main
VM Options: -Djava.library.path=/home/vzhilin/.local/opt/z3-z3-4.8.17/build
Program arguments: --file samples/matrix3x3_mod_26.txt --cols 3 --mod 26
Working directory: /home/vzhilin/.local/Programming/Initiative/z3-parser
Environment variables: LD_LIBRARY_PATH=/home/vzhilin/.local/opt/z3-z3-4.8.17/build
```

### Without IDEA
At first, build the distribution:
```
./gradlew clean installDist
```

Then change directory and invoke program like this:
```
cd build/install/z3-inv-matrix

LD_LIBRARY_PATH=/home/vzhilin/.local/opt/z3-z3-4.8.17/build \
JAVA_OPTS=-Djava.library.path=/home/vzhilin/.local/opt/z3-z3-4.8.17/build \
bin/z3-inv-matrix --file samples/matrix3x3_mod_26.txt --cols 3 --mod 26
```
