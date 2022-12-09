#!/bin/bash

name=$1
l_year=$2
r_year=$3
path=$4

# echo $1 $2 $3 $4
if [ "$r_year" == "" ] # 仅搜索名字 * < year < *
then
  path=$2
  grep -o "$name<\/author>" $path | wc -l
elif [ "$path" == "" ] # 搜索固定年份 year == 2012
then
  path=$3
  awk  -v FS='<year>|</year>' 'BEGIN{s=0}/'"$name"'<\/author>/,/<\/year>/{if($2=='"$l_year"') s+=1 }END{print s}' $path
elif [ "$l_year" == "-" ] # * < year < 2009
then
  awk  -v FS='<year>|</year>' 'BEGIN{s=0}/'"$name"'<\/author>/,/<\/year>/{if($2>1900&&$2<'"$r_year"') s+=1 }END{print s}' $path
elif [ "$r_year" == "-" ] # 2001 < year < *
then
  awk  -v FS='<year>|</year>' 'BEGIN{s=0}/'"$name"'<\/author>/,/<\/year>/{if($2>'"$l_year"') s+=1 }END{print s}' $path
else # 2001 < year < 2009
  awk  -v FS='<year>|</year>' 'BEGIN{s=0}/'"$name"'<\/author>/,/<\/year>/{if($2>'"$l_year"'&&$2<'"$r_year"') s+=1 }END{print s}' $path
fi