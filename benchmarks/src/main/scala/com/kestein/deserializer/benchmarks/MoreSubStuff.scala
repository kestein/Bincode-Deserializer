package com.kestein.deserializer.benchmarks

import com.fasterxml.jackson.databind.annotation.JsonSerialize

/*
pub enum MoreSubStuff {
    Less,
    More,
    Maybe,
    No(Option<usize>)
}
 */
@JsonSerialize(using = classOf[MoreSubStuffSerializer])
trait MoreSubStuff
