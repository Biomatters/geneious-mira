/*
 * Written by Bastien Chevreux (BaCh)
 *
 * Copyright (C) 2003 and later by Bastien Chevreux
 *
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 *
 */

#ifndef _bas_bloomfilter_h_
#define _bas_bloomfilter_h_

#include <iostream>

#include "stdinc/defines.H"
#include "mira/types_basic.H"
#include "errorhandling/errorhandling.H"

#if defined(_MSC_VER)
#define FORCE_INLINE    __forceinline
#else
#define FORCE_INLINE __attribute__((always_inline))
#endif


class BloomFilter
{
  //Variables
private:
  //static const bool BF_staticinitialiser;

  std::vector<uint8> BF_bloomfield;
  uint64        BF_bfaddressmask;

  uint64        BF_level1count;
  uint64        BF_level2count;

  uint64        BF_numuniqkmers;  // probable num unique kmers
  uint64        BF_numkmerseenge2;  // probable num kmers seen two or more times
  uint64        BF_numkmerseenge3;  // probable num kmers seen three or more times

  uint32        BF_numkeys;

public:


  //Functions
private:
  void init();

/*************************************************************************
 *
 * MurmurHash3_x64 variant:
 *  - 8 byte (64 bit) input (instead variable length)
 *  - seed is 64 bits (instead 32)
 *  - returning only 64 bits (instead 128)
 *
 * Code shamelessly copied and adapted from the copyright free code
 *  for Murmurhash3 by ... at http://code.google.com/p/smhasher/
 *
 *
 *************************************************************************/
  inline uint64_t rotl64(uint64_t x, int8_t r) { return (x << r) | (x >> (64 - r)); }

  FORCE_INLINE uint64 mmh3_64_8 ( const void * key, const uint64 seed ) {
    uint64_t h1 = seed;
    uint64_t h2 = seed;

    const uint8_t * tail = static_cast<const uint8_t*>(key);

    uint64_t k1 = uint64_t(tail[ 7]) << 56;
    k1 ^= uint64_t(tail[ 6]) << 48;
    k1 ^= uint64_t(tail[ 5]) << 40;
    k1 ^= uint64_t(tail[ 4]) << 32;
    k1 ^= uint64_t(tail[ 3]) << 24;
    k1 ^= uint64_t(tail[ 2]) << 16;
    k1 ^= uint64_t(tail[ 1]) << 8;
    k1 ^= uint64_t(tail[ 0]) << 0;

    k1 *= 0x87c37b91114253d5LLU;
    k1  = rotl64(k1,31);
    k1 *= 0x4cf5ad432745937fLLU;
    h1 ^= k1;

    h1 += h2;
    h2 += h1;

    h1 ^= h1 >> 33;
    h1 *= 0xff51afd7ed558ccdLLU;
    h1 ^= h1 >> 33;
    h1 *= 0xc4ceb9fe1a85ec53LLU;
    h1 ^= h1 >> 33;

    h2 ^= h2 >> 33;
    h2 *= 0xff51afd7ed558ccdLLU;
    h2 ^= h2 >> 33;
    h2 *= 0xc4ceb9fe1a85ec53LLU;
    h2 ^= h2 >> 33;

    h1 += h2;

    return h1;
  }

public:
  BloomFilter(const uint8 bits, const uint32 numhashes);
  BloomFilter(BloomFilter const &other);
  ~BloomFilter();

  BloomFilter const & operator=(BloomFilter const & other);
  friend std::ostream & operator<<(std::ostream &ostr, BloomFilter const &bf);

  void reset();
  void discard();

  uint64 getNumKMersSeenGE2() const { return BF_numkmerseenge2;}

  void addSequenceToBloomfield(const void * seqvoid,
			       uint64 slen,
			       const uint8 basesperhash,
			       const char * namestr);

  FORCE_INLINE bool isNonUnique(vhash_t dnahash){
    uint64 mmh3hash1=mmh3_64_8(&dnahash,0);
    uint64 mmh3hash2=mmh3_64_8(&mmh3hash1,mmh3hash1);

    uint64 mmh3ihash;
    uint64 indexinbf;
    uint32 bitinbf;
    for(uint16 numkeys=1; numkeys<=BF_numkeys; ++numkeys){
      mmh3ihash=(mmh3hash1+numkeys*mmh3hash2)&BF_bfaddressmask;
      indexinbf=mmh3ihash/4;
      bitinbf=static_cast<uint32>(4+(mmh3ihash%4));
      if(unlikely(!BITTEST(bitinbf,BF_bloomfield[indexinbf]))) return false;
    }
    return true;
  }

#define prefetchrl(p)     __builtin_prefetch((p), 0, 3)

  FORCE_INLINE void prefetchVHash(vhash_t dnahash){
    uint64 mmh3hash1=mmh3_64_8(&dnahash,0);
    uint64 mmh3hash2=mmh3_64_8(&mmh3hash1,mmh3hash1);

    uint64 mmh3ihash;
    uint64 indexinbf;

    for(uint16 numkeys=1; numkeys<=BF_numkeys; ++numkeys){
      mmh3ihash=(mmh3hash1+numkeys*mmh3hash2)&BF_bfaddressmask;
      indexinbf=mmh3ihash/4;
      prefetchrl(&(BF_bloomfield[indexinbf]));
    }
  }

/*************************************************************************
 *
 * retvalue=0 -> VHash was certainly not in filter before
 *         =1 -> VHash was perhaps at most once in filter
 *         =2 -> VHash was perhaps more than once in filter
 *
 *************************************************************************/

  FORCE_INLINE int addVHash(vhash_t dnahash){
    int retvalue=0;
    uint64 mmh3hash1=mmh3_64_8(&dnahash,0);
    uint64 mmh3hash2=mmh3_64_8(&mmh3hash1,mmh3hash1);

    uint16 setcounter=0;
    uint64 mmh3ihash;
    uint64 indexinbf;
    uint32 bitinbf;

    for(uint16 numkeys=1; numkeys<=BF_numkeys; ++numkeys){
      mmh3ihash=(mmh3hash1+numkeys*mmh3hash2)&BF_bfaddressmask;
      indexinbf=mmh3ihash/4;
      bitinbf=static_cast<uint32>(mmh3ihash%4);
      if(unlikely(BITTEST(bitinbf,BF_bloomfield[indexinbf]))) {
	++setcounter;
      }else{
	BITSET(bitinbf,BF_bloomfield[indexinbf]);
      }
    }
    BF_level1count+=BF_numkeys-setcounter;
    //std::cout << std::hex << dnahash << std::dec << "\tBF_level1count " << BF_level1count << "\tsetc: " << setcounter << std::endl;
    retvalue+=static_cast<int>(setcounter==BF_numkeys);
    if(setcounter==BF_numkeys){
      setcounter=0;
      for(uint16 numkeys=1; numkeys<=BF_numkeys; ++numkeys){
	mmh3ihash=(mmh3hash1+numkeys*mmh3hash2)&BF_bfaddressmask;
	indexinbf=mmh3ihash/4;
	bitinbf=static_cast<uint32>(4+(mmh3ihash%4));
	if(unlikely(BITTEST(bitinbf,BF_bloomfield[indexinbf]))){
	  ++setcounter;
	}else{
	  BITSET(bitinbf,BF_bloomfield[indexinbf]);
	}
      }
      BF_level2count+=BF_numkeys-setcounter;
      //std::cout << std::hex << dnahash << std::dec << "\tBF_level2count " << BF_level2count << "\tsetc: " << setcounter << std::endl;
      retvalue+=static_cast<uint8>(setcounter==BF_numkeys);
    }
    //std::cout << "retv: " << retvalue << std::endl;
    if(retvalue) {
      if(retvalue>1) {
	++BF_numkmerseenge3;
      }else{
	++BF_numkmerseenge2;
      }
    }else{
      ++BF_numuniqkmers;
    }
    return retvalue;
  }

};


#endif
