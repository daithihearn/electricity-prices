help:
	@egrep -h '\s#@\s' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?#@ "}; {printf "\033[36m  %-30s\033[0m %s\n", $$1, $$2}'

build-local: #@ Build local
	./gradlew clean build
build-image: #@ Build docker image
	docker build -t electricity-prices . --load