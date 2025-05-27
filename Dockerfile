FROM ubuntu:latest
LABEL authors="Nicholas"

ENTRYPOINT ["top", "-b"]