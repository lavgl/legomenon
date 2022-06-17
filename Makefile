.PHONY: dev build


dev:
	clj -M:dev

build:
	clj -T:build uber

clean:
	clj -T:build clean
