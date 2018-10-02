extern crate bincode;
extern crate clap;

use std::io::{BufReader, BufWriter, Read, Write, stdin, stdout};
use std::fs::File;

use bincode::serialize_into;
use clap::{Arg, App, SubCommand};

fn main() {
    let app_cli =   App::new("Bincode Tester")
                        .about("Used to benchmark and test the scala bincode converter from Rust")
                        .arg(Arg::with_name("OUTPUT")
                            .short("o")
                            .required(true)
                            .help("Path to write the output to. -/stdout will output to stdout")
                            .default_value("-")
                        )
                        .subcommand(SubCommand::with_name("write")
                            .about("Writes the specified number of records in bincode format to OUTPUT")
                            .arg(Arg::with_name("Amount")
                                .short("a")
                                .required(true)
                                .help("The number of records to write.")
                                .default_value("1000")
                            )
                        )
                        .subcommand(SubCommand::with_name("convert")
                            .arg(Arg::with_name("INPUT")
                                .short("i")
                                .required(true)
                                .help("Path to read data from. -/stdin will output to stdin")
                                .default_value("stdin")
                            )
                            .about("Converts bincode records from INPUT to json records in OUTPUT")
                        )
                    .get_matches();
    let sink: Box<Write> = match app_cli.value_of("OUTPUT").unwrap_or("-") {
        "-" | "stdout" => Box::new(stdout()),
        s @ _ => Box::new(BufWriter::new(File::create(s).expect("Unable to create file")))
    };
    match app_cli.subcommand() {
        ("write", Some(sub_args)) => {
            let amount = sub_args.value_of("Amount")
                                 .unwrap_or("1000")
                                 .parse()
                                 .expect("Unable to parse string to usize");
            write(sink, amount);
        },
        ("convert", Some(sub_args)) => {
            let source: Box<Read> = match app_cli.value_of("OUTPUT").unwrap_or("-") {
                "-" | "stdin" => Box::new(stdin()),
                s @ _ => Box::new(BufReader::new(File::open(s).expect("Unable to create file")))
            };
            convert(sink, source);
        },
        _ => {
            panic!("No subcommand selected. Please select write or convert.")
        }
    }
}

fn write<W: Write>(sink: W, amount: usize) {
    
}

fn convert<W: Write, R: Read>(sink: W, source: R) {
    
}
