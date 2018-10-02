extern crate bincode;
extern crate bincode_tester;
extern crate clap;
extern crate serde_json;

use std::io::{BufReader, BufWriter, Read, Write, stdin, stdout};
use std::fs::File;

use bincode::{deserialize_from, serialize_into};
use bincode_tester::{Harness, RandomStuff};
use clap::{Arg, App, SubCommand};
use serde_json::to_vec;

const DEFAULT_WRITE: &'static str = "1";
const DEFAULT_SOURCE_AND_SINK: &'static str = "-";
const DEFAULT_SEED: &'static str = "asdfasdf";

fn main() {
    let app_cli =   App::new("Bincode Tester")
                        .about("Used to benchmark and test the scala bincode converter from Rust")
                        .arg(Arg::with_name("OUTPUT")
                            .short("o")
                            .required(true)
                            .help("Path to write the output to. -/stdout will output to stdout")
                            .default_value(DEFAULT_SOURCE_AND_SINK)
                        )
                        .arg(Arg::with_name("SEED")
                            .short("s")
                            .help("Seed to use for randomly generated data")
                            .default_value(DEFAULT_SEED)
                        )
                        .subcommand(SubCommand::with_name("write")
                            .about("Writes the specified number of records in bincode format to OUTPUT")
                            .arg(Arg::with_name("Amount")
                                .short("a")
                                .required(true)
                                .help("The number of records to write.")
                                .default_value(DEFAULT_WRITE)
                            )
                        )
                        .subcommand(SubCommand::with_name("convert")
                            .arg(Arg::with_name("INPUT")
                                .short("i")
                                .required(true)
                                .help("Path to read data from. -/stdin will output to stdin")
                                .default_value(DEFAULT_SOURCE_AND_SINK)
                            )
                            .about("Converts bincode records from INPUT to json records in OUTPUT")
                        )
                    .get_matches();
    let sink: Box<Write> = match app_cli.value_of("OUTPUT").unwrap_or("-") {
        "-" | "stdout" => Box::new(stdout()),
        s @ _ => Box::new(BufWriter::new(File::create(s).expect("Unable to create file")))
    };
    let mut harness = Harness::new(sink, "");
    match app_cli.subcommand() {
        ("write", Some(sub_args)) => {
            let amount = sub_args.value_of("Amount")
                                 .unwrap()
                                 .parse()
                                 .expect("Unable to parse string to usize");
            write(&mut harness, amount);
        },
        ("convert", Some(sub_args)) => {
            let source: Box<Read> = match sub_args.value_of("INPUT").unwrap() {
                "-" | "stdin" => Box::new(stdin()),
                s @ _ => Box::new(BufReader::new(File::open(s).expect("Unable to create file")))
            };
            convert(&mut harness, source);
        },
        _ => {
            panic!("No subcommand selected. Please select write or convert.")
        }
    }
}

fn write<W: Write>(harness: &mut Harness<W>, amount: usize) {
    for _ in 0..amount {
        let rand_data = harness.get_data();
        serialize_into(&mut harness.sink, &rand_data).expect("Unable to serialize data");
    }
}

fn convert<W: Write, R: Read>(harness: &mut Harness<W>, mut source: R) {
    while let Ok(data) = deserialize_from(&mut source) {
        harness.sink.write_all(
            to_vec::<RandomStuff>(&data)
                                .expect("Unable to json encode data")
                                .as_slice()
            )
        .expect("Unable to write converted json data");
    }
}
