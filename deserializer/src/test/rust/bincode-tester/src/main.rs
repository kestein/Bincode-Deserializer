extern crate bincode;
extern crate bincode_tester;
extern crate clap;
extern crate serde_json;

use std::io::{
    BufRead, BufReader, BufWriter, Read, Write, stdin, stdout
};
// use std::iter::Zip;
use std::fs::File;

use bincode::{deserialize_from, serialize_into};
use bincode_tester::{Harness, RandomStuff};
use clap::{Arg, App, SubCommand};
use serde_json::{Value, from_str, to_vec};

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
                        .subcommand(SubCommand::with_name("validate")
                            .arg(Arg::with_name("BASE")
                                .short("b")
                                .required(true)
                                .help("Path of a base file with valid output (from Rust)")
                                .default_value("")
                            )
                            .arg(Arg::with_name("AGAINST")
                                .short("a")
                                .required(true)
                                .help("Path of a file to validate (from scala)")
                                .default_value("")
                            )
                        )
                        .about("Compares 2 json files to ensure they have the same output.
                                Use one file from scala and one from rust.")
                    .get_matches();
    let sink: Box<dyn Write> = match app_cli.value_of("OUTPUT").unwrap_or("-") {
        "-" | "stdout" => Box::new(stdout()),
        s @ _ => Box::new(BufWriter::new(File::create(s).expect("Unable to create file")))
    };
    let mut harness = Harness::new(sink, app_cli.value_of("SEED").unwrap());
    match app_cli.subcommand() {
        ("write", Some(sub_args)) => {
            let amount = sub_args.value_of("Amount")
                                 .unwrap()
                                 .parse()
                                 .expect("Unable to parse string to usize");
            write(&mut harness, amount);
        },
        ("convert", Some(sub_args)) => {
            let source: Box<dyn Read> = match sub_args.value_of("INPUT").unwrap() {
                "-" | "stdin" => Box::new(stdin()),
                s @ _ => Box::new(BufReader::new(File::open(s).expect("Unable to create file")))
            };
            convert(&mut harness, source);
        },
        ("validate", Some(sub_args)) => {
            let from_rust = BufReader::new(File::open(sub_args.value_of("BASE")
                                                              .expect("Argument for BASE not provided")
                                                    ).expect("Unable to open BASE file")
            );
            let from_scala = BufReader::new(File::open(sub_args.value_of("AGAINST")
                                                              .expect("Argument for AGAINST not provided")
                                                    ).expect("Unable to open AGAINST file")
            );
            harness.sink.write(validate(from_rust, from_scala).as_bytes())
                        .expect("Unable to write validation output");
        }
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
        ).expect("Unable to write converted json data");
        harness.sink.write_all(b"\n").expect("Unable to write json newline separator");
    }
}

fn validate(from_rust: BufReader<File>, from_scala: BufReader<File>) -> String {
    let mut rust_lines = from_rust.lines();
    let mut scala_lines = from_scala.lines();
    let mut lines_validated = 1;
    loop {
        let rust_value: Value;
        if let Some(Ok(r)) = rust_lines.next() {
            rust_value = from_str(r.as_str()).expect("Unable to serialize Rust value");
        } else {
            break
        }
        let scala_value: Value;
        if let Some(Ok(s)) = scala_lines.next() {
            scala_value = from_str(s.as_str()).expect("Unable to serialize Rust value");
        } else {
            // There was a rust value, but no scala value. BASE has more data than AGAINST
            return String::from("AGAINST has fewer lines than BASE")
        }
        if rust_value != scala_value {
            return format!("The 2 files have unequal values on line {}", lines_validated)
        }
        lines_validated += 1;
    }
    // The rust file has been fully processed. See if there are any more scala lines
    if let Some(Ok(s)) =  scala_lines.next() {
        // The AGAINST file has more data in it. See if it is valid.
        match from_str::<Value>(s.as_str()) {
            Ok(_) => String::from("AGAINST has more lines than BASE"),
            Err(_) => String::from("AGAINST has more, unserializable data than BASE")
        }
    } else {
        // Both files are the same
        String::new()
    }
}
