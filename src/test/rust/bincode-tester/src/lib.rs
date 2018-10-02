extern crate rand;
#[macro_use]
extern crate serde_derive;

use std::collections::HashMap;
use std::io::Write;

use rand::{
    Rng, SeedableRng,
    rngs::StdRng,
};

pub struct Harness<W: Write> {
    data: RandomStuffGenerator,
    pub sink: W
}

impl<W: Write> Harness<W> {
    pub fn new(sink: W, seed: &str) -> Harness<W> {
        Harness {
            data: RandomStuffGenerator::new(seed),
            sink: sink
        }
    }

    pub fn get_data(&mut self) -> RandomStuff {
        self.data.gen_RandomStuff()
    }
}

pub struct RandomStuffGenerator {
    generator: StdRng
}

impl RandomStuffGenerator {
    pub fn new(seed: &str) -> RandomStuffGenerator {
        RandomStuffGenerator{
            generator: StdRng::from_seed(str_to_u32_array(seed.as_bytes()))
        }
    }

    pub fn gen_RandomStuff(&mut self) -> RandomStuff {
        RandomStuff {
            one: HashMap::new(),
            two: Vec::new(),
            three: 0,
            four: false,
            five: 0.0,
            six: 0,
            seven: None,
            eight: (0, 0, 0, 0)
        }
    }

    pub fn gen_RandomSubStuff(&mut self) -> RandomSubStuff {
        RandomSubStuff {
            one: 0,
            two: String::new(),
            three: 64,
            four: Vec::new(),
            five: MoreSubStuff::Less
        }
    }

    pub fn gen_MoreSubStuff(&mut self) -> MoreSubStuff {
        MoreSubStuff::Less
    }
}

#[derive(Deserialize, Serialize)]
pub struct RandomStuff {
    one: HashMap<usize, RandomSubStuff>,
    two: Vec<String>,
    three: i8,
    four: bool,
    five: f64,
    six: u32,
    seven: Option<i64>,
    eight: (u8, i16, u32, i64),
}

#[derive(Deserialize, Serialize)]
pub struct RandomSubStuff {
    one: usize,
    two: String,
    three: i64,
    four: Vec<Vec<u16>>,
    five: MoreSubStuff
}

#[derive(Deserialize, Serialize)]
pub enum MoreSubStuff {
    Less,
    More,
    Maybe,
    No(Option<usize>)
}

fn str_to_u32_array(seed: &[u8]) -> [u8; 32] {
    let mut set_seed = [0u8; 32];
    for i in 0..seed.len() {
        if i > 31 {
            break
        }
        set_seed[i] = seed[i];
    }
    set_seed
}
