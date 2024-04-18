package com.suttori.handler;

import java.util.List;

public interface Handler<T> {
    void choose(T t);

    void choose(List<T> t);
}
