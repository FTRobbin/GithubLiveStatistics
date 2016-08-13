# GithubLiveStatistics

A Java-based system to implement efficient Github Crawling to completely replace the old [GithubCrawler](https://github.com/FTRobbin/GithubCrawler), also designed to support user-customed statistics to be plugged-in at runtime.

The goal of this project is to reach Version 1.2 that comes with a Http-front end and friendly documents so users can add and monitor their own statistics.

However, the demands changed from the original "Crawl all repos and run various analysis across Github" to "Pinpoint a few repos with given keyword", thus the design of GLS goes too cumbersome for this specific propose. So the project was aborted at version 0.6.2, which is a working prototype that could crawl and clone Github Repo with the APIs 25x faster than the original GithubCrawler and very robust against errors, but only implemented the most basic statistics with a not-so-pretty socket console front. The GLS is later replaced by a series of light-weight scripts and has been dead ever since.

Authored by [Haobin Ni](https://github.com/FTRobbin) 2016 July