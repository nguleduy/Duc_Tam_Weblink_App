//////////////////////////////////////////////////////////////////////////////////////////////
///
/// @file ByteBuffer.cpp
/// @brief
///
/// Contains implementation of the CByteBuffer class.
///
/// @author Abalta Technologies, Inc.
/// @date 8/2011
///
/// @cond Copyright
///
/// COPYRIGHT 2011 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
/// This program may not be reproduced, in whole or in part in any form or any means whatsoever
/// without the written permission of ABALTA TECHNOLOGIES.
///
/// @endcond
//////////////////////////////////////////////////////////////////////////////////////////////


//--------------------------------------------------------------------------------------------
//  Includes
//--------------------------------------------------------------------------------------------
#include "ByteBuffer.h"


//--------------------------------------------------------------------------------------------
//  Module and debug definitions
//--------------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------------
//  Local type declarations and macros
//--------------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------------
//  Constants
//--------------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------------
//  Function Prototypes
//--------------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------------
//  Implementation
//--------------------------------------------------------------------------------------------


CByteBuffer::CByteBuffer()
: m_buffer(NULL)
, m_size(0)
, m_pos(0)
, m_capacity(0)
, m_hasOwnership(true)
{
}

CByteBuffer::CByteBuffer(const CByteBuffer& buffer)
: m_buffer(NULL)
, m_size(0)
, m_pos(0)
, m_capacity(0)
, m_hasOwnership(true)
{
  unsigned size = buffer.GetSize();
  if (size > 0 && Resize(size))
  {
    memcpy(m_buffer, buffer.m_buffer, size);
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////
/// Constructs new instance of CByteBuffer
/// 
/// @param[in]  buf  - 
/// @param[in]  size - 
//////////////////////////////////////////////////////////////////////////////////////////////
CByteBuffer::CByteBuffer(const char* buf, unsigned size, bool shallowCopy)
: m_buffer(NULL)
, m_size(0)
, m_pos(0)
, m_capacity(0)
, m_hasOwnership(true)
{
  SetBuffer(buf, size, shallowCopy);
}

CByteBuffer::CByteBuffer(unsigned size)
: m_buffer(NULL)
, m_size(0)
, m_pos(0)
, m_capacity(0)
, m_hasOwnership(true)
{
  if (SetCapacity(size))
  {
    m_size = size;
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////
/// Destructor
/// 
//////////////////////////////////////////////////////////////////////////////////////////////
CByteBuffer::~CByteBuffer()
{
  if (m_hasOwnership && NULL != m_buffer)
  {
    free (m_buffer);
  }
  m_buffer = NULL;
}

bool CByteBuffer::SetBuffer(const char* buf, unsigned size, bool shallowCopy)
{
  bool result = true;
  if (m_hasOwnership && NULL != m_buffer)
  {
    free (m_buffer);
  }

  m_buffer   = NULL;
  m_size     = 0;
  m_pos      = 0;
  m_capacity = 0;
  m_hasOwnership = !shallowCopy || buf == NULL;
  if (buf != NULL)
  {
    if (m_hasOwnership)
    {
      result = SetCapacity(size);
      if (result)
      {
        m_size = size;
        memcpy(m_buffer, buf, size);
      }
    }
    else
    {
      m_buffer   = (char*)buf;
      m_size     = size;
      m_pos      = 0;
      m_capacity = size;
    }
  }
  return result;
}

void CByteBuffer::TakeOwnership()
{
  m_hasOwnership = true;
}

bool CByteBuffer::AddBytes(const char buffer[], unsigned bufSize)
{
  bool result = false;

  if (buffer != NULL && bufSize > 0)
  {
    result = true;

    if (!m_hasOwnership)
    {
      result = CopyOnWrite();
    }
    if (result)
    {
      unsigned requiredCapacity = m_size + m_pos + bufSize;

      if (requiredCapacity > m_capacity)
      {
        // request some extra space
        result = SetCapacity(((requiredCapacity + 2) * 3) / 2);
      }
      if (result)
      {
        memcpy(m_buffer + m_size, buffer, bufSize);
        m_size += bufSize;
      }
    }
  }
  else
  {
    result = bufSize == 0;
  }
  return result;
}

bool CByteBuffer::Resize( unsigned size )
{
  bool result = false;

  size += m_pos;
  if (size <= m_capacity)
  {
    m_size = size;
    result = true;
  }
  else if (size > m_capacity)
  {
    result = SetCapacity(size);
    if (result)
    {
      m_size = size;
    }
  }
  return result;
}

bool CByteBuffer::SetCapacity( unsigned capacity )
{
  bool  result = true;

  if (!m_hasOwnership)
  {
    result = CopyOnWrite();
  }
  if (result)
  {
    char* newPtr = (char*)((capacity > 0 || m_buffer != NULL) ? realloc(m_buffer, capacity) : NULL);

    result = newPtr != NULL || capacity == 0;
    assert(result);
    if (result)
    {
      m_buffer   = newPtr;
      m_capacity = capacity;
      if (m_size > m_capacity)
      {
        m_size = m_capacity;
        if (m_pos > m_size)
        {
          m_pos = m_size;
        }
      }
    }
  }
  return result;
}

bool CByteBuffer::CopyOnWrite()
{
  bool result = true;

  if (!m_hasOwnership)
  {
    char* newPtr = NULL;

    assert(m_size <= m_capacity);
    if (m_buffer != NULL && m_size > 0)
    {
      newPtr = (char*)malloc(m_capacity);   
      if (newPtr)
      {
        memcpy(newPtr, m_buffer, m_size);
      }
      else
      {
        result = false;
      }
    }
    assert(result);
    if (result)
    {
      m_buffer       = newPtr;
      m_hasOwnership = true;
    }
  }
  return result;
}

bool CByteBuffer::Normalize()
{
  bool result = true;
  if (!m_hasOwnership)
  {
    result = CopyOnWrite();
  }
  if (m_pos > 0 && m_size > 0 && m_size > m_pos)
  {
    memmove(m_buffer, m_buffer + m_pos, m_size - m_pos);
    m_size -= m_pos;
    m_pos   = 0;
  }
  else if (m_pos == m_size)
  {
    m_pos = m_size = 0;
  }
  return result;
}

