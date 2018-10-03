extern crate rand;
#[macro_use]
extern crate serde_derive;

use std::collections::HashMap;
use std::io::Write;
use std::iter::FromIterator;

use rand::{
    Rng, SeedableRng,
    distributions::Standard,
    rngs::StdRng,
};

const MIN: usize = 0;
const MAX_COLLECTION_LEN: usize = 20;
const MAX_STR_LEN: usize = 200;

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

    #[allow(non_snake_case)]
    pub fn gen_RandomStuff(&mut self) -> RandomStuff {
        let mut one = HashMap::new();
        for _ in  0..self.generator.gen_range(MIN, MAX_COLLECTION_LEN) {
            one.insert(self.generator.gen(), self.gen_RandomSubStuff());
        }
        let mut two = Vec::new();
        for _ in 0..self.generator.gen_range(MIN, MAX_STR_LEN) {
            two.push(self.gen_string());
        }
        let mut seven = None;
        if self.generator.gen::<usize>()%2 == 0 {
            seven = Some(self.generator.gen::<i64>());
        }
        let mut eight = (0, 0, 0, 0);
        eight.0 = self.generator.gen();
        eight.1 = self.generator.gen();
        eight.2 = self.generator.gen();
        eight.3 = self.generator.gen();
        RandomStuff {
            one: one,
            two: two,
            three: self.generator.gen(),
            four: self.generator.gen::<usize>()%2 == 0,
            five: self.generator.gen::<f64>(),
            six: self.generator.gen::<u32>(),
            seven: seven,
            eight: eight
        }
    }

    #[allow(non_snake_case)]
    pub fn gen_RandomSubStuff(&mut self) -> RandomSubStuff {
        let mut four = Vec::new();
        for _ in 0..self.generator.gen_range(MIN, MAX_COLLECTION_LEN) {
            let sub_length: usize = self.generator.gen_range(MIN, MAX_COLLECTION_LEN);
            four.push(self.generator.sample_iter(&Standard)
                                    .take(sub_length)
                                    .collect()
            );
        }
        RandomSubStuff {
            one: self.generator.gen(),
            two: self.gen_string(),
            three: self.generator.gen(),
            four: four,
            five: self.gen_MoreSubStuff()
        }
    }

    #[allow(non_snake_case)]
    pub fn gen_MoreSubStuff(&mut self) -> MoreSubStuff {
        match self.generator.gen::<usize>()%4 {
            0 => MoreSubStuff::Less,
            1 => MoreSubStuff::More,
            2 => MoreSubStuff::Maybe,
            3 => {
                if self.generator.gen::<usize>()%2 == 0 {
                    MoreSubStuff::No(Some(self.generator.gen()))
                } else {
                    MoreSubStuff::No(None)
                }
            },
            _ => unreachable!()
        }
    }

    fn gen_string(&mut self) -> String {
        let strlen = self.generator.gen_range(MIN,MAX_STR_LEN);
        let chars: Vec<char> = self.generator.sample_iter(&Standard).take(strlen).collect();
        String::from_iter(chars)
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
