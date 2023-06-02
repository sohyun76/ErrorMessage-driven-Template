## Preliminary Study on the Reproducibility of Fix Templates in Static Analysis Tools
\# Overview
--------------
ErrorMessage-driven-Template utilizes [TBar](https://github.com/TruX-DTF/TBar), one of the open-source tools for template-based APR.

I. Requirement
--------------
- Ubuntu 20.04.3 LTS (Focal Fossa)
- Java 11
- PostgreSQL 13+
    - `ErrorMessage-driven-Template/src/main/java/edu/lu/uni/serval/tbar/utils/DBUtils.java`
    ```
        private static String url = "jdbc:postgresql://localhost:xxxx/";
        private static String user = "sonar";
        private static String password = "password";
        private static String dbSonarqube = "sonarqube";
        private static String dbResults = "validation_results";
        private static String tableResults = "validation_result";
    ```
- SonarQube 9.1
    - Community Edition Version 9.1 (build 47736)
    - `ErrorMessage-driven-Template/src/main/java/edu/lu/uni/serval/tbar/utils/ShellUtils.java`
    ```
        private static String snqbToken = "sonarqube_token";
        private static String snqbIp = "xxx.xxx.xxx.xxx";
        private static String snqbPort = "xxxx";
    ```
- SonarScanner 4.6.2.2472
- Apache Maven 3.6.3


II. Dataset
--------------
ErrorMessage-driven-Template uses [Sorald-Experiments](https://github.com/khaes-kth/Sorald-experiments/blob/master/considered_repos_stats.csv) dataset.
- ./considered_repos_stats.csv

III. Quick Start
--------------
```
$ git clone https://github.com/sohyun76/ErrorMessage-driven-Template.git 
$ cd ErrorMessage-driven-Template
$ ./compile.sh run ./D4J/projects/Project_79
```
