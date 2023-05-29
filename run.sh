#! /bin/bash

POC_HOME=`pwd`
project_dir=$1
project_name=${project_dir##*/}
parent_dir=${project_dir/"$project_name"/""}
parent_dir="$POC_HOME/$parent_dir"
echo $project_dir "$project_name" $parent_dir


java -Xmx1800g -cp "target/dependency/*" edu.lu.uni.serval.tbar.main.MainPerfectFL $parent_dir "$project_name" ./BugPositions.txt
