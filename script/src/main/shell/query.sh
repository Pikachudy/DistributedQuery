#!/bin/bash

name=$1
l_year=$2
r_year=$3
path=$4

# echo $1 $2 $3 $4
if [ "$r_year" == "" ] # 仅搜索名字 * < year < *
then
  path=$2
  grep -o "$name" $path | wc -l
elif [ "$path" == "" ] # 搜索固定年份 year == 2012
then
  path=$3
  awk -v FS="<author>|</author>|<year>|</year>" -v name="$name" -v year="$l_year" '$2==name&&$4==year{s++}END{print s}' $path
elif [ "$l_year" == "-" ] # * < year < 2009
then
  awk -v FS="<name>|</name>|<year>|</year>" -v name="$name" -v year="$r_year" '$2==name&&$4<year{s++}END{print s}' $path
elif [ "$r_year" == "-" ] # 2001 < year < *
then
  awk -v FS="<name>|</name>|<year>|</year>" -v name="$name" -v year="$l_year" '$2==name&&$4>year{s++}END{print s}' $path
else # 2001 < year < 2009
  awk -v FS="<name>|</name>|<year>|</year>" -v name="$name" -v l_year="$l_year" -v r_year="$r_year" '$2==name&&$4>l_year&&$4<r_year{s++}END{print s}' $path
fi