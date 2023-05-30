# Template-based APR(TBar+SonarQube) for PoC

## **Overview**

본 레포지토리는 템플릿기반 프로그램자동수정(APR, Automated Program Repiar)의 개념검증을 위한 코드이다. 결함위치식별(FL, Fault Localization) 및 후보패치검증(PV, Patch Validation)에 정적분석도구 SonarQube를 활용했으며, 패치생성(PG, Patch Generation)에 템플릿기반 APR의 오픈소스 도구 중 하나인 TBar를 활용했다.

## **PoC 환경**

- Ubuntu 20.04.3 LTS (Focal Fossa)
- Java 11
    ```shell
    $ java -version
    openjdk version "11.0.11" 2021-04-20
    OpenJDK Runtime Environment (build 11.0.11+9-Ubuntu-0ubuntu2.20.04)
    OpenJDK 64-Bit Server VM (build 11.0.11+9-Ubuntu-0ubuntu2.20.04, mixed mode, sharing)
    ```
- PostgreSQL 13+
    ```shell
    $ psql --version
    psql (PostgreSQL) 13.5 (Ubuntu 13.5-1.pgdg20.04+1)
    ```
- SonarQube 9.1
    - Community Edition Version 9.1 (build 47736)
- SonarScanner 4.6.2.2472
- Apache Maven 3.6.3
- 기타
  - `sudo apt-get install wget curl zip build-essential subversion`

## **Quick Start**

```
$ export POC_HOME="~/TBar_PoC"
$ git clone https://github.ecodesamsung.com/SE-APR/TBar_PoC.git $POC_HOME
$ cd $POC_HOME
$ ./DownloadAOSPs.sh
$ ./compile.sh run ./aosp/Calculator_1
```

- 주의사항
    - `$POC_HOME` 내부에서 `mvn clean` 진행시 TBar에서 제공한 라이브러리가 모두 삭제되므로 `mvn clean` 명령어 사용시 현재 디렉토리위치에 주의
    - 모든 변경사항은 [3ab7329](https://github.ecodesamsung.com/SE-APR/TBar_PoC/commit/3ab7329f04005a44f9e2040c55d92fb543e3cde0) commit에 merge되어 있으며, [d1b5333](https://github.ecodesamsung.com/SE-APR/TBar_PoC/commit/d1b533306211a5737b92cc33c9fd239f2ba21a8f) commit은 TBar의 최신 커밋 임
    - 모든 변경사항의 시작과 끝은 아래와 같은 형식의 주석으로 둘러싸여있음
    (신규 생성한 클래스의 경우 별도로 아래와 같은 형식의 주석처리를 하지 않음)
        
        ```
        Type.1 Code Modification
        // selab: origin
        ...
        // selab: modified code
        ...
        // selab: end
        
        Type.2 Add Imports
        // selab: Add imports
        ...
        // selab: end
        ```
        

## Environmental Setup

### PostgreSQL 설정

- DB 관련 설정값은 전역변수로 코드 내에 구현됨
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/utils/DBUtils.java`
- 관련 커멘드는 본 문서 최하단의 Msc. > PostgreSQL 항목 참조

### SonarQube 설정

- 접근 관련 설정값은 전역변수로 코드 내에 구현됨
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/utils/ShellUtils.java`
- 관련 커멘드는 본 문서 최하단의 Msc. > SonarQube 항목 참조
    - 해당 커멘드 중 export 문 실행 필수

## **How It Works**

![TBar_PoC_Overview](figure/TBar_PoC_Overview.png)

### **APR-0 PoC 실행**

- PoC의 편의상 TBar의 main 클래스들 중 `edu.lu.uni.serval.tbar.main.MainPerfectFL` 클래스를 주 변경 시작점으로 사용
    - MainPerfectFL 클래스에서 FL, PG, PV 과정을 수행
    `$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/main/MainPerfectFL.java`
- 기본적으로 PoC를 실행하는 shellscript은 $POC_HOME/run.sh 사용
`POC_HOME $ ./run.sh /path/to/target/project
POC_HOME $ ./run.sh ./aosp/Calculator_1`
- 코드에 수정사항이 발생한 경우 $POC_HOME/compile.sh 사용
`POC_HOME $ ./compile.sh # build 만 수행`
`POC_HOME $ ./compile.sh run ./aosp/Calculator_1 # build 후 실행`

### FL
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:localizeDefects()`

#### **FL-1 SonarQube 프로젝트 생성**

- SonarQube Web API 사용
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/utils/ShellUtils.java:shellCreateProject()`

#### **FL-2 대상 프로젝트에 대해 SonarQube 실행(SonarScanner 사용)**

- 대상 프로젝트에 대해 정적분석 결과 생성
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/utils/ShellUtils.java:RunSonarQube()`

#### **FL-3 SonarQube 분석 결과로부터 의심되는 스테이트먼트(Suspicious Statements) 확보**

- FL의 결과물로 이슈별 결함 파일 및 라인 정보 수집
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/utils/DBUtils.java:getSuspStmt()`
- 현재는 버그타입(issue_type=2) 과 룰 1,3,5 에 대해서 수행
    - JIRA 이슈#6 에서 분석요청한 룰1-5 중 현재 PoC 대상 프로젝트 8개에서 룰 2,4는 검출되지 않아 제외

#### **FL-4 의심되는 스테이트먼트 정보를 TBar에 입력 데이터로 변환 및 로드(load)**

- 데이터 변환 및 로드
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:readKnownBugPositionsFromFile()`
- TBar의 입력 형태
    
    `{ProjectName:String}_{BugID:String}@/path/to/suspicious/file.java@{suspiciousLine:Integer}`
    `Calculator_1@src/com/android/calendar/DayView.java@3105`
    

### **PG**
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:generatePatches()`
#### **PG-1 결함 정보 추출**
- 주어진 결함 정보(파일 + 라인) 별로 AST(Abstract Syntax Tree) 분석 및 노드 추출
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:generatePatches()`
#### **PG-2 템플릿 선정**
- AST 노드 별로 맵핑된 템플릿(fix template) 선별
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:fixWithMatchedFixTemplates()`
    - 템플릿 위치
    `POC_HOME/src/main/java/edu/lu/uni/serval/tbar/fixtemplate/FixTemplate.java`
    `POC_HOME/src/main/java/edu/lu/uni/serval/tbar/fixpatterns/*.java`
#### **PG-3 후보 패치 생성**
- 결함 라인에 템플릿 적용하여 후보 패치 생성
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:generateAndValidatePatches()`
주의: 기존 함수명으로 따라감에 따라 validation 의 키워드가 유지되어 있으나, 실질적인 후보패치 검증 코드는 없음
    - 패치 클래스
    `$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/info/Patch.java`

### PV
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:validatePatches()`

#### **PV-1 후보패치를 대상 프로젝트에 적용**

`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/AbstractFixer.java:addPatchCodeToFile()`

#### **PV-2 SonarQube 실행**

- SonarQube Web API 사용
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/utils/ShellUtils.java:shellCreateProject()`

#### **PV-3 SonarQube 분석 결과 후처리**

- 다음 데이터를 추출
: patchID, issueID, fixPattern, 신규/유지/제거 이슈목록, 패치적용에 대한 git diff 
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/TBarFixer.java:validatePatches()`

#### **PV-4 SonarQube 분석 결과 DB에 저장**

- 로컬 PostgreSQL에 DB를 생성하고 저장
`$POC_HOME/src/main/java/edu/lu/uni/serval/tbar/utils/DBUtils.java:insertValidationResult()`

## **Contact**

성균관대학교 소프트웨어공학 연구실 ([https://selab.skku.ac.kr](http://selab.skku.ac.kr))

정호현 [jeonghh89@skku.edu](mailto:jeonghh89@skku.edu), [good92489@gmail.com](mailto:good92489@gmail.com)

허진석 [mrhjs225@skku.edu](mailto:mrhjs225@skku.edu)

## Msc.

### PostgreSQL

```bash
sudo apt-get install -y lsb-release net-tools && sudo apt-get clean all

wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -  

sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'

sudo apt-get update
sudo apt-get install -y postgresql

sudo service postgresql start
sudo service postgresql status
sudo -u postgres psql -c "SELECT version();"

su - postgres
createuser sonar
psql
psql$ ALTER USER sonar WITH ENCRYPTED PASSWORD 'dummypasswd';
psql$ CREATE DATABASE sonarqube OWNER sonar;
psql$ CREATE DATABASE validation_results OWNER sonar;
psql$ GRANT ALL PRIVILEGES ON DATABASE sonarqube to sonar;
psql$ GRANT ALL PRIVILEGES ON DATABASE validation_results to sonar;
\q
exit
sudo service postgresql restart
netstat -tulpena | grep postgres
```

### SonarQube

```bash
wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-9.1.0.47736.zip
unzip sonarqube-9.1.0.47736.zip
mv sonarqube-9.1.0.47736 /somewhere/sonarqube

vi sonarqube/conf/sonar.properties
# in vi
sonar.jdbc.username=sonar
sonar.jdbc.password=dummypasswd
sonar.jdbc.url=jdbc:postgresql://localhost/sonarqube
sonar.search.javaOpts=-Xmx512m -Xms512m -XX:+HeapDumpOnOutOfMemoryError
sonar.web.host=0.0.0.0
sonar.web.port=9000
# end vi

/somewhere/sonarqube/bin/linux-x86-64/sonar.sh console

# Sonar-Scanner
wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.6.2.2472.zip
unzip sonar-scanner-cli-4.6.2.2472.zip
mv sonar-scanner-cli-4.6.2.2472 /somewhere/sonar-scanner
export PATH=$PATH:/somewhere/sonar-scanner/bin
```